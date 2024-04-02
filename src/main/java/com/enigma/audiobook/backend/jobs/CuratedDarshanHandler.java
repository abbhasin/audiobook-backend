package com.enigma.audiobook.backend.jobs;

import com.enigma.audiobook.backend.dao.CuratedDarshanDao;
import com.enigma.audiobook.backend.dao.DarshanDao;
import com.enigma.audiobook.backend.dao.GodDao;
import com.enigma.audiobook.backend.dao.MandirDao;
import com.enigma.audiobook.backend.models.ContentUploadStatus;
import com.enigma.audiobook.backend.models.CuratedDarshan;
import com.enigma.audiobook.backend.models.Darshan;
import com.enigma.audiobook.backend.models.God;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;

@Component
@AllArgsConstructor
@Slf4j
public class CuratedDarshanHandler implements Runnable {
    final CuratedDarshanDao curatedDarshanDao;
    final GodDao godDao;
    final MandirDao mandirDao;
    final DarshanDao darshanDao;

    // config
    static final int DEFUALT_DARSHAN_COUNT_PER_GOD = 2;
    static final Map<String, Integer> darshanCountPerGod = new TreeMap<>();

    static {
        darshanCountPerGod.put("Guru Nanak Ji", 3);
        darshanCountPerGod.put("Shiva", 2);
        darshanCountPerGod.put("Vishnu", 2);
    }

    public void updateDarshanCountPerGod(String godName, Integer count) {
        darshanCountPerGod.put(godName, count);
    }

    @Override
    public void run() {
        Map<String, God> allGodsByName = godDao.getGods(100)
                .stream()
                .collect(Collectors.toMap(God::getGodName, god -> god));

        Optional<CuratedDarshan> lastCuratedDarshan = curatedDarshanDao.getLastNCuratedDarshan(1)
                .stream()
                .findFirst();

        Map<String, List<String>> nextGodToDarshans = new TreeMap<>();
        AtomicReference<String> lastGodsMandir = new AtomicReference<>();

        if (lastCuratedDarshan.isPresent()) {
            Map<String, List<String>> godToDarshans = lastCuratedDarshan.get().getGodToDarshanIds();
            godToDarshans = new TreeMap<>(godToDarshans);

            godToDarshans.forEach((godName, darshansForGod) -> {
                God god = allGodsByName.get(godName);

                String lastDarshanId = (!darshansForGod.isEmpty()) ?
                        darshansForGod.get(darshansForGod.size() - 1) : null;

                Integer countOfDarshansToFetchForGod = darshanCountPerGod.getOrDefault(god.getGodName(),
                        DEFUALT_DARSHAN_COUNT_PER_GOD);

                updateDarshans(nextGodToDarshans, god, lastDarshanId,
                        lastGodsMandir, countOfDarshansToFetchForGod);
            });
        }

        Set<String> godNamesWithDarshan = nextGodToDarshans.keySet();
        Set<String> allGodNames = allGodsByName.keySet();
        Set<String> godNamesToFetchDarshans = new HashSet<>(allGodNames);
        godNamesToFetchDarshans.removeAll(godNamesWithDarshan);

        lastGodsMandir.set(null);

        godNamesToFetchDarshans
                .forEach(godName -> {
                    God god = allGodsByName.get(godName);
                    Integer countOfDarshansToFetchForGod = darshanCountPerGod.getOrDefault(god.getGodName(),
                            DEFUALT_DARSHAN_COUNT_PER_GOD);

                    updateDarshans(nextGodToDarshans, god, null,
                            lastGodsMandir, countOfDarshansToFetchForGod);
                });

        CuratedDarshan curatedDarshan = new CuratedDarshan();
        curatedDarshan.setGodToDarshanIds(nextGodToDarshans);
        curatedDarshanDao.addCuratedDarshan(curatedDarshan);
    }

    private void updateDarshans(Map<String, List<String>> nextGodToDarshans,
                                God god,
                                String lastDarshanId,
                                AtomicReference<String> lastGodsMandir,
                                int countOfDarshansToFetchForGod) {
        List<Darshan> darshans = getRoundRobinDarshans(
                god.getGodId(),
                lastDarshanId,
                lastGodsMandir.get(),
                countOfDarshansToFetchForGod);

        nextGodToDarshans.put(god.getGodName(), darshans.stream().map(Darshan::getDarshanId).toList());

        lastGodsMandir.set(darshans
                .stream()
                .findFirst()
                .map(Darshan::getMandirId)
                .orElse(lastGodsMandir.get()));
    }

    /**
     * Rules:
     * 1. first try to find for god greater than last darshan id excluding the mandir that is already present
     * in the new list
     * 2. Then if not found enough, try to remove the condition of excluding the mandir
     * 3. if not found enough, start from the first darshan of the god
     */
    private List<Darshan> getRoundRobinDarshans(String godId,
                                                String lastDarshanId,
                                                String excludingMandir,
                                                int countOfDarshansToFetchForGod) {
        List<Darshan> darshans = getDarshanByGodAfterLastDarshanExcludingMandir(
                godId,
                lastDarshanId,
                excludingMandir,
                countOfDarshansToFetchForGod
        );

        darshans = new ArrayList<>(darshans.stream().distinct().toList());

        if (darshans.size() < countOfDarshansToFetchForGod) {
            darshans.addAll(getDarshanByGodAfterLastDarshan(
                    godId,
                    lastDarshanId,
                    countOfDarshansToFetchForGod - darshans.size()
            ));
        }

        darshans = new ArrayList<>(darshans.stream().distinct()
                .sorted(Comparator.comparing(d -> new ObjectId(d.getDarshanId())))
                .toList());

        if (darshans.size() < countOfDarshansToFetchForGod) {
            darshans.addAll(getDarshanByGod(
                    godId,
                    countOfDarshansToFetchForGod - darshans.size()
            ));
        }

        darshans = new ArrayList<>(darshans.stream().distinct().toList());
        return darshans;
    }

    private List<Darshan> getDarshanByGodAfterLastDarshanExcludingMandir(String godId,
                                                                         String lastDarshanId,
                                                                         String excludingMandir,
                                                                         int limit) {
        return darshanDao.getDarshanByGod(
                godId,
                ContentUploadStatus.PROCESSED,
                Optional.ofNullable(lastDarshanId),
                Optional.ofNullable(excludingMandir),
                limit);
    }

    private List<Darshan> getDarshanByGodAfterLastDarshan(String godId,
                                                          String lastDarshanId,
                                                          int limit) {
        return darshanDao.getDarshanByGod(
                godId,
                ContentUploadStatus.PROCESSED,
                Optional.ofNullable(lastDarshanId),
                Optional.empty(),
                limit);
    }

    private List<Darshan> getDarshanByGod(String godId,
                                          int limit) {
        return darshanDao.getDarshanByGod(
                godId,
                ContentUploadStatus.PROCESSED,
                Optional.empty(),
                Optional.empty(),
                limit);
    }
}
