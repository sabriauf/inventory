package lk.sabri.inventory.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.Date;
import java.util.List;

@Dao
public interface CustomerDAO {

    @Query("SELECT * FROM Customer order by custName")
    List<Customer> getAll();

    @Query("SELECT * FROM Customer WHERE isInserted = 1")
    List<Customer> getUserInserted();

    @Query("SELECT * FROM Customer WHERE custId IN (:customers)")
    List<Customer> loadAllByIds(int[] customers);

    @Query("SELECT * FROM Customer WHERE custId IN (:customers)")
    Customer loadAllById(int customers);

    @Query("SELECT * FROM Customer WHERE custName LIKE :first")
    List<Customer> findByName(String first);

    @Query("SELECT COUNT(custId) FROM Customer")
    int getCount();

    @Query("UPDATE Customer SET isInserted = 0 WHERE isInserted = 1")
    void updateUserStatus();

    @Insert
    void insertAll(Customer... customers);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Customer> customers);

    @Delete
    void delete(Customer customer);

    @Delete
    void deleteUserInserted(List<Customer> customer);
}
