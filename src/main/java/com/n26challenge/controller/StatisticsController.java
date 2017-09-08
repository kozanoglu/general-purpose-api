package com.n26challenge.controller;

import com.n26challenge.domain.StatisticsResult;
import com.n26challenge.service.StatisticsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/statistics/")
public class StatisticsController {

    @Autowired
    private StatisticsService statisticsService;

    @RequestMapping(method = RequestMethod.GET)
    @ResponseStatus(HttpStatus.OK)
    public StatisticsResult getStatistics() {

        return statisticsService.getStatisticsForTheLastMinute();
    }
}
