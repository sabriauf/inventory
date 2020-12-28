package lk.sabri.inventory.data;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface LoginDataDAO {

    @Query("SELECT * FROM LoginData")
    List<LoginData> getAll();

    @Query("SELECT * FROM LoginData WHERE username IN (:username)")
    LoginData loadAllByUsername(String username);

    @Query("SELECT COUNT(id) FROM LoginData")
    int getCount();

    @Insert
    void insertAll(LoginData... data);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<LoginData> data);

    @Delete
    void delete(LoginData data);
}
