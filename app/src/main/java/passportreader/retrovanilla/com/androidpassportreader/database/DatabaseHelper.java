package passportreader.retrovanilla.com.androidpassportreader.database;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

/**
 * Created by antonio on 16/12/15.
 */
public class DatabaseHelper extends SQLiteOpenHelper {

    private static DatabaseHelper instance;

    private static final String DATABASE_NAME = "passportreader.db";

    public static final String DOCUMENT_NUMBER = "documentID";
    public static final String BIRTH_DATE = "birthDate";
    public static final String EXPIRATION_DATE = "expirationDate";
    public static final String MAC = "mac";
    public static final String MESSAGE = "message";

    public DatabaseHelper(Context context){
        super(context, DATABASE_NAME, null, 1);
    }

    @Override
    public void onCreate(SQLiteDatabase db){
        try{
            db.execSQL("CREATE TABLE IF NOT EXISTS document (" +
                    DOCUMENT_NUMBER+" TEXT PRIMARY KEY NOT NULL, " +
                    BIRTH_DATE+" TEXT NOT NULL, " +
                    EXPIRATION_DATE+" TEXT NOT NULL," +
                    MAC+" BLOB NULL," +
                    MESSAGE+" BLOB NULL);");
        }
        catch(Exception e){
            Log.e("ERROR", "Failed to create tables of the database: " + e.getStackTrace());
        }
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion){
        try{
            db.execSQL("DROP TABLE IF EXISTS document");
            onCreate(db);
        }
        catch(Exception e){
            Log.e("ERROR", "Failed to drop the tables of the database: " + e.getStackTrace());
        }
    }

    public static synchronized DatabaseHelper getHelper(Context context){
        if (instance == null)
            instance = new DatabaseHelper(context);

        return instance;
    }
}
