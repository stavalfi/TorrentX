package main.statistics;

import reactor.core.publisher.Flux;

public interface SpeedStatistics {

    double getRateInMillSeconds();

    Flux<Double> getDownloadSpeedFlux();

    Flux<Double> getUploadSpeedFlux();
}
