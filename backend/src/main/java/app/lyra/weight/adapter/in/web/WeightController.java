package app.lyra.weight.adapter.in.web;

import app.lyra.shared.domain.UserId;
import app.lyra.weight.application.port.in.DeleteWeightUseCase;
import app.lyra.weight.application.port.in.ListWeightsUseCase;
import app.lyra.weight.application.port.in.RecordWeightUseCase;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/weights")
@RequiredArgsConstructor
class WeightController {

    private final RecordWeightUseCase recordWeight;
    private final ListWeightsUseCase listWeights;
    private final DeleteWeightUseCase deleteWeight;

    @GetMapping
    List<WeightResponse> list(UserId userId) {
        return listWeights.listWeights(userId).stream().map(WeightResponse::from).toList();
    }

    @PutMapping("/{date}")
    WeightResponse recordWeight(UserId userId, @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                @RequestBody WeightRequest request) {
        return WeightResponse.from(recordWeight.recordWeight(userId, date, request.kilograms()));
    }

    @DeleteMapping("/{date}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    void delete(UserId userId, @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        deleteWeight.delete(userId, date);
    }
}
