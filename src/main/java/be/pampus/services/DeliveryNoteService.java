package be.pampus.services;

import be.pampus.data.DeliveryNote;
import be.pampus.data.DeliveryNoteRepository;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

@Service
public class DeliveryNoteService {

    private final DeliveryNoteRepository repository;

    public DeliveryNoteService(DeliveryNoteRepository repository) {
        this.repository = repository;
    }

    public Optional<DeliveryNote> get(Long id) {
        return repository.findById(id);
    }

    public DeliveryNote update(DeliveryNote entity) {
        return repository.save(entity);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }

    public Page<DeliveryNote> list(Pageable pageable) {
        return repository.findAll(pageable);
    }

    public Page<DeliveryNote> list(Pageable pageable, Specification<DeliveryNote> filter) {
        return repository.findAll(filter, pageable);
    }

    public int count() {
        return (int) repository.count();
    }

}
