package com.kozanoglu.controller.statistics;

import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import com.kozanoglu.model.statistics.Transaction;
import com.kozanoglu.service.statistics.StatisticsService;

import static com.kozanoglu.util.TimeUtil.isWithinLastMinute;

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

    public static void main(String[] args) {
        System.out.println(new Date().getTime());
    }
}
