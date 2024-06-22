package be.pampus.data;


import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface DeliveryNoteRepository
        extends
            JpaRepository<DeliveryNote, Long>,
            JpaSpecificationExecutor<DeliveryNote> {

}
