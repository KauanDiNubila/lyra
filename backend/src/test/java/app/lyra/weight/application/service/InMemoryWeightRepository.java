package app.lyra.weight.application.service;

import app.lyra.shared.domain.UserId;
import app.lyra.weight.application.port.out.WeightRepository;
import app.lyra.weight.domain.WeightEntry;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

class InMemoryWeightRepository implements WeightRepository {

    private final Map<UserId, Map<LocalDate, WeightEntry>> weightsByOwner = new HashMap<>();

    @Override
    public void save(UserId owner, WeightEntry entry) {
        weightsByOwner.computeIfAbsent(owner, any -> new TreeMap<>()).put(entry.date(), entry);
    }

    @Override
    public List<WeightEntry> findAll(UserId user) {
        return List.copyOf(weightsByOwner.getOrDefault(user, Map.of()).values());
    }

    @Override
    public boolean delete(UserId user, LocalDate date) {
        Map<LocalDate, WeightEntry> weights = weightsByOwner.get(user);
        return weights != null && weights.remove(date) != null;
    }
}
