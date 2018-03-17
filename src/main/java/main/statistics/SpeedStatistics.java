package main.statistics;

import reactor.core.publisher.Flux;

public interface SpeedStatistics {
    Flux<Double> getDownloadSpeedFlux();

    Flux<Double> getUploadSpeedFlux();
}
