package com.kozanoglu.service.statistics;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

import com.kozanoglu.model.statistics.StatisticPerSecond;
import com.kozanoglu.model.statistics.StatisticsResult;
import com.kozanoglu.model.statistics.Transaction;

import static com.kozanoglu.util.TimeUtil.getSecondFromTimestamp;
import static com.kozanoglu.util.TimeUtil.isWithinLastMinute;

@Service
public class StatisticsService {

    private static final Map<Integer, StatisticPerSecond> STATISTICS = new ConcurrentHashMap<>();

    /**
     * Persists the transaction to a hash map if it's within the last minute.
     * In order to achieve constant time complexity we keep a concurrent hash map for the last 60 seconds.
     * We outdate the old transactions and merge the transactions fall into same second at every call.
     *
     * @param transaction transaction dto
     */
    public void persistTransaction(Transaction transaction) {

        if (!isWithinLastMinute(transaction.getTimestamp())) {
            return;
        }

        cleanOldTransactions();

        int secondFromTimestamp = getSecondFromTimestamp(transaction.getTimestamp());

        if (STATISTICS.containsKey(secondFromTimestamp)) {
            mergeStatisticsPerSecond(secondFromTimestamp, transaction);
        } else {
            createNewStatisticPerSecond(secondFromTimestamp, transaction);
        }
    }

    /**
     * Filters and aggregates last 60 seconds transaction objects
     *
     * @return StatisticsResult object
     */
    public StatisticsResult getStatisticsForTheLastMinute() {
        Double min = STATISTICS.size() == 0 ? 0 : Double.MAX_VALUE;
        Double max = 0.0;
        Double totalAmount = 0.0;
        Double average = 0.0;
        Integer totalCount = 0;

        for (StatisticPerSecond statistic : STATISTICS.values()) {

            if (!isWithinLastMinute(statistic.getTimestamp())) {
                continue;
            }

            if (statistic.getMin() < min) {
                min = statistic.getMin();
            }

            if (statistic.getMax() > max) {
                max = statistic.getMax();
            }

            totalAmount += statistic.getSum();
            totalCount += statistic.getCount();
        }

        if (totalCount > 0)
        {
            average = totalAmount / totalCount;
        }

        StatisticsResult statisticsResult = new StatisticsResult();
        statisticsResult.setAvg(average);
        statisticsResult.setCount(totalCount);
        statisticsResult.setMax(max);
        statisticsResult.setMin(min);
        statisticsResult.setSum(totalAmount);
        return statisticsResult;
    }

    /**
     * Used by test classes to clean the static map before each test run
     */
    public void clearStatistics() {
        STATISTICS.clear();
    }

    /**
     * Used by test classes.
     */
    Map<Integer, StatisticPerSecond> getStatistics() {
        HashMap<Integer, StatisticPerSecond> newMap = new HashMap<>();
        newMap.putAll(STATISTICS);
        return newMap;
    }

    private void mergeStatisticsPerSecond(int second, Transaction transaction) {

        StatisticPerSecond statistic = STATISTICS.get(second);

        statistic.setSum(statistic.getSum() + transaction.getAmount());
        statistic.setCount(statistic.getCount() + 1);

        if (transaction.getAmount() > statistic.getMax()) {
            statistic.setMax(transaction.getAmount());
        }

        if (transaction.getAmount() < statistic.getMin()) {
            statistic.setMin(transaction.getAmount());
        }
    }

    private void createNewStatisticPerSecond(int second, Transaction transaction) {
        StatisticPerSecond statistic = new StatisticPerSecond();
        statistic.setTimestamp(transaction.getTimestamp());
        statistic.setMin(transaction.getAmount());
        statistic.setMax(transaction.getAmount());
        statistic.setSum(transaction.getAmount());
        statistic.setCount(1);
        STATISTICS.put(second, statistic);
    }

    /**
     * Whenever we persist a new transaction we call this method and remove all the outdated transactions.
     * Since the map size is can't be bigger than 60 this method runs at constant time.
     */
    private void cleanOldTransactions() {
        STATISTICS.entrySet().removeIf(t -> !isWithinLastMinute(t.getValue().getTimestamp()));
    }
}
