package com.n26challenge.controller;

import com.n26challenge.domain.Transaction;
import com.n26challenge.service.StatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import static com.n26challenge.util.TimeUtil.isWithinLastMinute;

@RestController
@RequestMapping(value = "/transactions")
public class TransactionsController {

    @Autowired
    private StatisticsService statisticsService;

    @RequestMapping(method = RequestMethod.POST, headers = {
            "content-type=application/json"})
    public ResponseEntity<Transaction> persistTransaction(@RequestBody final Transaction transaction) {
        if (transaction == null || transaction.getAmount() == null || transaction.getTimestamp() == null) {
            return ResponseEntity.status(HttpStatus.I_AM_A_TEAPOT).build();
        }

        if (!isWithinLastMinute(transaction.getTimestamp())) {
            return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
        }

        statisticsService.persistTransaction(transaction);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }
}
