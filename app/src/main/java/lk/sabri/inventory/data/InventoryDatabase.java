package lk.sabri.inventory.data;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.TypeConverters;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

import lk.sabri.inventory.util.DateTypeConverter;

@Database(entities = {Customer.class, Invoice.class, InvoiceItem.class, Item.class, LoginData.class, Payment.class}, version = 6)
@TypeConverters({DateTypeConverter.class})
public abstract class InventoryDatabase extends RoomDatabase {

    private static InventoryDatabase db;

    public static InventoryDatabase getInstance(Context context) {
        if (db == null)
            db = Room.databaseBuilder(context.getApplicationContext(), InventoryDatabase.class, "inventory_db")
                    .addMigrations(MIGRATION_4_5)
                    .fallbackToDestructiveMigration().build();
        return db;
    }

    static final Migration MIGRATION_4_5 = new Migration(4, 5) {
        @Override
        public void migrate(SupportSQLiteDatabase database) {
            database.execSQL("CREATE TABLE 'Payment' ('id' INTEGER NOT NULL, "
                    + "'date' INTEGER, 'invoiceId' TEXT, 'details' TEXT, 'method' TEXT, 'amount' DOUBLE NOT NULL, PRIMARY KEY('id'))");
        }
    };

    public abstract CustomerDAO customerDAO();

    public abstract InvoiceDAO invoiceDAO();

    public abstract InvoiceItemDAO invoiceItemDAO();

    public abstract ItemDAO itemDAO();

    public abstract LoginDataDAO loginDAO();

    public abstract PaymentDAO paymentDAO();
}
