package lk.sabri.inventory.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface ItemDAO {

    @Query("SELECT * FROM Item")
    List<Item> getAll();

    @Query("SELECT * FROM Item WHERE itemId IN (:items)")
    Item loadAllById(int items);

    @Query("SELECT COUNT(itemId) FROM Item")
    int getCount();

    @Insert
    void insertAll(Item... items);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<Item> items);

    @Delete
    void delete(Item item);
}
