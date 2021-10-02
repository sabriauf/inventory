package lk.sabri.inventory.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.Date;
import java.util.List;

@Dao
public interface PaymentDAO {

    @Query("SELECT * FROM Payment order by date")
    List<Payment> getAll();

    @Query("SELECT * FROM Payment WHERE invoiceId=:id ORDER BY date")
    List<Payment> getPaymentsForInvoice(String id);

    @Query("SELECT * FROM Payment WHERE date > :time")
    List<Payment> getAll(Date time);

    @Insert
    void insertAll(Payment... payments);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Payment> payments);

    @Delete
    void delete(Payment payment);
}
