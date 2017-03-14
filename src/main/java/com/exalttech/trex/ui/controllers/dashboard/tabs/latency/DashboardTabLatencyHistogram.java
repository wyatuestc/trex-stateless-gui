package com.exalttech.trex.ui.controllers.dashboard.tabs.latency;

import javafx.collections.FXCollections;
import javafx.concurrent.WorkerStateEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.layout.AnchorPane;
import javafx.stage.WindowEvent;
import javafx.util.Duration;

import java.util.*;

import com.exalttech.trex.ui.models.json.stats.latency.JSONStatsStream;
import com.exalttech.trex.ui.models.json.stats.latency.JSONStatsLatency;
import com.exalttech.trex.ui.views.services.RefreshingService;
import com.exalttech.trex.ui.views.statistics.StatsLoader;
import com.exalttech.trex.util.Constants;
import com.exalttech.trex.util.Initialization;
import com.exalttech.trex.util.StatsUtils;
import com.exalttech.trex.util.Util;


public class DashboardTabLatencyHistogram extends AnchorPane {
    @FXML
    private AnchorPane root;
    @FXML
    private BarChart histogram;
    @FXML
    private CategoryAxis xAxis;

    private RefreshingService refreshingService;
    private Set<Integer> visiblePorts;

    public DashboardTabLatencyHistogram() {
        Initialization.initializeFXML(this, "/fxml/Dashboard/tabs/latency/DashboardTabLatencyHistogram.fxml");

        refreshingService = new RefreshingService();
        refreshingService.setPeriod(Duration.seconds(Constants.REFRESH_ONE_INTERVAL_SECONDS));
        refreshingService.setOnSucceeded(this::onRefreshSucceeded);
        refreshingService.start();

        Initialization.initializeCloseEvent(root, this::onWindowCloseRequest);
    }

    public void setVisiblePorts(Set<Integer> visiblePorts) {
        this.visiblePorts = visiblePorts;
        onRefreshSucceeded(null);
    }

    private void onRefreshSucceeded(WorkerStateEvent event) {
        histogram.getData().clear();

        Set<String> visibleStreams = StatsUtils.getVisibleStream(visiblePorts);
        if (visibleStreams != null && visibleStreams.isEmpty()) {
            return;
        }

        Map<String, String> latencyStatsByStreams = StatsLoader.getInstance().getLatencyStatsMap();

        List<XYChart.Series> seriesList = new LinkedList<XYChart.Series>();
        Set<String> categories = new HashSet<String>();
        latencyStatsByStreams.forEach((String stream, String jsonLatencyStats) -> {
            if (visibleStreams != null && !visibleStreams.contains(stream)) {
                return;
            }

            JSONStatsStream latencyStats = (JSONStatsStream) Util.fromJSONString(
                    jsonLatencyStats,
                    JSONStatsStream.class
            );
            if (latencyStats == null) {
                return;
            }

            JSONStatsLatency latency = latencyStats.getLatency();
            if (latency == null) {
                return;
            }

            Map<String, Integer> histogram = latency.getHistogram();
            if (histogram == null) {
                return;
            }

            XYChart.Series series = new XYChart.Series();
            series.setName(stream);
            histogram.forEach((String key, Integer value) -> {
                categories.add(key);
                series.getData().add(new XYChart.Data<String, Number>(key, value));
            });
            seriesList.add(series);
        });

        if (seriesList.isEmpty()) {
            return;
        }

        histogram.getData().addAll(seriesList);
        List<String> categoriesList = new ArrayList<String>();
        categoriesList.addAll(categories);
        categoriesList.sort(new Comparator<String>() {
            @Override
            public int compare(String category1, String category2) {
                return Integer.parseInt(category1) - Integer.parseInt(category2);
            }
        });
        xAxis.setCategories(FXCollections.observableArrayList(categoriesList));
        xAxis.setAutoRanging(true);
    }

    private void onWindowCloseRequest(WindowEvent window) {
        if (refreshingService.isRunning()) {
            refreshingService.cancel();
        }
    }
}