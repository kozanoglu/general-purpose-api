package com.n26challenge.service;

import com.n26challenge.domain.StatisticPerSecond;
import com.n26challenge.domain.StatisticsResult;
import com.n26challenge.domain.Transaction;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Date;
import java.util.Map;

import static com.n26challenge.util.TimeUtil.getSecondFromTimestamp;

@RunWith(MockitoJUnitRunner.class)
public class StatisticsServiceTest {

    @InjectMocks
    private StatisticsService statisticsService;

    @Before
    public void clean()
    {
        statisticsService.clearStatistics();
    }

    @Test
    public void shouldPersistThreeSeparateTransactionsWithinTheLastMinute() {

        // Given
        Date now = new Date();

        Long time1 = now.getTime();
        Long time2 = now.getTime() - 1000;
        Long time3 = now.getTime() - 2000;
        Long time4 = 123456578L;

        Transaction transaction1 = new Transaction(1.2, time1);
        Transaction transaction2 = new Transaction(1.5, time2);
        Transaction transaction3 = new Transaction(1.8, time3);
        Transaction transaction4 = new Transaction(99999.9, time4);

        // When
        statisticsService.persistTransaction(transaction1);
        statisticsService.persistTransaction(transaction2);
        statisticsService.persistTransaction(transaction3);
        statisticsService.persistTransaction(transaction4);

        // Then
        Map<Integer, StatisticPerSecond> statistics = statisticsService.getStatistics();
        Assertions.assertThat(statistics.values()).hasSize(3);

        StatisticPerSecond statistic1 = statistics.get(getSecondFromTimestamp(time1));
        Assertions.assertThat(statistic1.getSum()).isEqualTo(1.2);
        StatisticPerSecond statistic2 = statistics.get(getSecondFromTimestamp(time2));
        Assertions.assertThat(statistic2.getSum()).isEqualTo(1.5);
        StatisticPerSecond statistic3 = statistics.get(getSecondFromTimestamp(time3));
        Assertions.assertThat(statistic3.getSum()).isEqualTo(1.8);
        StatisticPerSecond statistic4 = statistics.get(getSecondFromTimestamp(time4));
        Assertions.assertThat(statistic4).isNull();
    }

    @Test
    public void shouldPersistAndMergeTransactionsWithinTheLastMinute() {

        // Given
        Date now = new Date();
        Transaction transaction1 = new Transaction(1.2, now.getTime());
        Transaction transaction2 = new Transaction(1.5, now.getTime());
        Transaction transaction3 = new Transaction(1.8, now.getTime());
        Transaction transaction4 = new Transaction(99999.9, 123456578L);

        // When
        statisticsService.persistTransaction(transaction1);
        statisticsService.persistTransaction(transaction2);
        statisticsService.persistTransaction(transaction3);
        statisticsService.persistTransaction(transaction4);

        // Then
        Map<Integer, StatisticPerSecond> statistics = statisticsService.getStatistics();
        Assertions.assertThat(statistics.values()).hasSize(1);

        StatisticPerSecond statistic = statistics.values().iterator().next();
        Assertions.assertThat(statistic.getMin()).isEqualTo(1.2);
        Assertions.assertThat(statistic.getMax()).isEqualTo(1.8);
        Assertions.assertThat(statistic.getSum()).isEqualTo(4.5);
        Assertions.assertThat(statistic.getCount()).isEqualTo(3);
    }

    @Test
    public void shouldReturnTheStatisticsForTheLastMinute() {

        // Given
        statisticsService.clearStatistics();

        Date now = new Date();
        Transaction transaction1 = new Transaction(1.2, now.getTime());
        Transaction transaction2 = new Transaction(1.5, now.getTime());
        Transaction transaction3 = new Transaction(1.8, now.getTime());
        Transaction transaction4 = new Transaction(99999.9, 123456578L);

        // When
        statisticsService.persistTransaction(transaction1);
        statisticsService.persistTransaction(transaction2);
        statisticsService.persistTransaction(transaction3);
        statisticsService.persistTransaction(transaction4);

        // Then
        StatisticsResult statisticsResult = statisticsService.getStatisticsForTheLastMinute();
        Assertions.assertThat(statisticsResult.getMin()).isEqualTo(1.2);
        Assertions.assertThat(statisticsResult.getMax()).isEqualTo(1.8);
        Assertions.assertThat(statisticsResult.getSum()).isEqualTo(4.5);
        Assertions.assertThat(statisticsResult.getAvg()).isEqualTo(1.5);
        Assertions.assertThat(statisticsResult.getCount()).isEqualTo(3);
    }
}
