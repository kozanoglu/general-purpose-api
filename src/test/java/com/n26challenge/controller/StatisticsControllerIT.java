package com.n26challenge.controller;

import com.n26challenge.domain.StatisticsResult;
import com.n26challenge.domain.Transaction;
import com.n26challenge.service.StatisticsService;
import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.junit4.SpringRunner;

import java.util.Calendar;
import java.util.Date;

@RunWith(SpringRunner.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public class StatisticsControllerIT {

    @Autowired
    private TransactionsController transactionsController;
    @Autowired
    private StatisticsController statisticsController;
    @Autowired
    private StatisticsService statisticsService;

    @Before
    public void clean()
    {
        statisticsService.clearStatistics();
    }

    @Test
    public void shouldReturnExpectedStatistics() {

        // Given
        Date now = new Date();
        Transaction transaction1 = new Transaction(1.2, now.getTime());
        Transaction transaction2 = new Transaction(1.5, now.getTime());
        Transaction transaction3 = new Transaction(1.8, now.getTime());
        Transaction transaction4 = new Transaction(99999.9, 123456578L);

        transactionsController.persistTransaction(transaction1);
        transactionsController.persistTransaction(transaction2);
        transactionsController.persistTransaction(transaction3);
        transactionsController.persistTransaction(transaction4);

        // When
        StatisticsResult statistics = statisticsController.getStatistics();

        // Then
        Assertions.assertThat(statistics.getMin()).isEqualTo(1.2);
        Assertions.assertThat(statistics.getMax()).isEqualTo(1.8);
        Assertions.assertThat(statistics.getCount()).isEqualTo(3);
        Assertions.assertThat(statistics.getSum()).isEqualTo(4.5);
        Assertions.assertThat(statistics.getAvg()).isEqualTo(1.5);
    }

    @Test
    public void shouldIgnoreOutdatedTransactions() throws InterruptedException {

        // Given
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.SECOND, (calendar.get(Calendar.SECOND) - 57));

        Date fiftySevenSecondsAgo = calendar.getTime();
        Transaction transaction1 = new Transaction(1.2, fiftySevenSecondsAgo.getTime());
        Transaction transaction2 = new Transaction(1.5, fiftySevenSecondsAgo.getTime());
        Transaction transaction3 = new Transaction(1.8, fiftySevenSecondsAgo.getTime());
        Transaction transaction4 = new Transaction(1.9, fiftySevenSecondsAgo.getTime());
        Transaction transaction5 = new Transaction(1.0, new Date().getTime());

        transactionsController.persistTransaction(transaction1);
        transactionsController.persistTransaction(transaction2);
        transactionsController.persistTransaction(transaction3);
        transactionsController.persistTransaction(transaction4);
        transactionsController.persistTransaction(transaction5);

        // Wait for an additional 4 seconds so the first 4 transactions will be 61 seconds old
        Thread.sleep(4000);

        // When
        StatisticsResult statistics = statisticsController.getStatistics();

        // Then
        Assertions.assertThat(statistics.getMin()).isEqualTo(1.0);
        Assertions.assertThat(statistics.getMax()).isEqualTo(1.0);
        Assertions.assertThat(statistics.getCount()).isEqualTo(1);
        Assertions.assertThat(statistics.getSum()).isEqualTo(1.0);
        Assertions.assertThat(statistics.getAvg()).isEqualTo(1.0);
    }

}
