package be.pampus.data;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Lob;
import java.time.LocalDate;

@Entity
public class DeliveryNote extends AbstractEntity {

    private LocalDate date;
    @Lob
    @Column(length = 1000000)
    private byte[] price;

    public LocalDate getDate() {
        return date;
    }
    public void setDate(LocalDate date) {
        this.date = date;
    }
    public byte[] getPrice() {
        return price;
    }
    public void setPrice(byte[] price) {
        this.price = price;
    }

}
