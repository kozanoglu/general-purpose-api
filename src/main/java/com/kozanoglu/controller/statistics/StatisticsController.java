package com.kozanoglu.controller.statistics;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.kozanoglu.model.statistics.StatisticsResult;
import com.kozanoglu.service.statistics.StatisticsService;

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
