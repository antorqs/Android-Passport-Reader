package passportreader.retrovanilla.com.androidpassportreader.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ParseException;
import android.util.Log;

import org.jmrtd.BACKeySpec;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Vector;

import passportreader.retrovanilla.com.androidpassportreader.data.Document;

/**
 * Database Handler
 * Created by antonio on 16/12/15.
 */
public class DatabaseHandler {

    private Context context;

    public DatabaseHandler(Context context){
        this.context = context;
    }

    /**
     *
     * @param document
     */
    public void insertDocument(Document document){
        try{
            //Open the database
            DatabaseHelper databaseHelper = DatabaseHelper.getHelper(context);
            SQLiteDatabase db = databaseHelper.getWritableDatabase();

            try{
                //Create the object to insert in the database
                ContentValues cv = new ContentValues();
                cv.put(DatabaseHelper.DOCUMENT_NUMBER, document.getDocumentNumber());
                cv.put(DatabaseHelper.BIRTH_DATE, document.getDateOfBirth());
                cv.put(DatabaseHelper.EXPIRATION_DATE, document.getDateOfExpiry());
                cv.put(DatabaseHelper.MAC, document.getMac());
                cv.put(DatabaseHelper.MESSAGE, document.getMessage());

                //Insert the values of the ContentValues object in the table of the database
                Log.d("DEBUG", "Inserting in the ePassport table");
                db.insert("document", DatabaseHelper.DOCUMENT_NUMBER, cv);

            } catch (ParseException e) {
                Log.e("ERROR", "ERROR: Failed to format the date: " + e.getStackTrace());
            }

            //Close the database
            db.close();
        }
        catch(Exception e){
            Log.e("ERROR", "ERROR: Failed to insert in the ePassport table: " + e.getStackTrace());
        }
    }

    /**
     *
     * @param document
     */
    public void updateDocument(Document document){
        try{
            //Open the database
            DatabaseHelper databaseHelper = DatabaseHelper.getHelper(context);
            SQLiteDatabase db = databaseHelper.getWritableDatabase();

            try{
                //Create the object to update in the database
                ContentValues cv = new ContentValues();
                cv.put(DatabaseHelper.BIRTH_DATE, document.getDateOfBirth());
                cv.put(DatabaseHelper.EXPIRATION_DATE, document.getDateOfExpiry());
                cv.put(DatabaseHelper.MAC, document.getMac());
                cv.put(DatabaseHelper.MESSAGE, document.getMessage());
                String whereConstrait =
                        DatabaseHelper.DOCUMENT_NUMBER + "="
                                +"\""+document.getDocumentNumber()+"\"";

                //Insert the values of the ContentValues object in the table of the database
                db.update("document", cv, whereConstrait, null);


            } catch (ParseException e) {
                Log.e("ERROR", "ERROR: Failed to format the date: " + e.getStackTrace());
            }

            //Close the database
            db.close();
        }
        catch(Exception e){
            Log.e("ERROR", "ERROR: Failed to update the ePassport table: " + e.getStackTrace());
        }
    }

    /**
     *
     * @param document
     */
    public void deleteDocument(Document document){
        try{
            //Open the database
            DatabaseHelper databaseHelper = DatabaseHelper.getHelper(context);
            SQLiteDatabase db = databaseHelper.getWritableDatabase();

            try{
                String whereConstrait = DatabaseHelper.DOCUMENT_NUMBER
                        + "= \"" + document.getDocumentNumber()+"\"";

                db.delete("document", whereConstrait, null);

            } catch (ParseException e) {
                Log.e("ERROR", "ERROR: Failed to format the date: " + e.getStackTrace());
            }
            //Close the database
            db.close();
        }
        catch(Exception e){
            Log.e("ERROR", "ERROR: Failed to delete a record of the document table: " + e.getStackTrace());
        }
    }
    /**
     *
     * @return List with all the documents in the database
     */
    public List<BACKeySpec> getDocumentsAsBACKeys(){
        Vector<BACKeySpec> result = new Vector<BACKeySpec>();
        try{
            //Open the database
            DatabaseHelper databaseHelper = DatabaseHelper.getHelper(context);
            SQLiteDatabase db = databaseHelper.getWritableDatabase();

            //Perform the query and store it in an Cursor object
            String select = "SELECT * FROM document";
            Cursor cursor = db.rawQuery(select,null);

            if(cursor.moveToFirst()){
                do{
                    try{
                        //create a new instance of credential passing it the documentId, the birth date and the expiry date retrieved from the DB
                        Document document =
                                new Document(cursor.getString(0), cursor.getString(1),
                                        cursor.getString(2), cursor.getBlob(3), cursor.getBlob(4), context);
                        result.add(document);
                    } catch (ParseException e) {
                        Log.e("ERROR", "ERROR: Failed to parse the date: " + e.getStackTrace());
                    }
                } while(cursor.moveToNext());
            }

            //Close Cursor object
            cursor.close();

            //Close database
            db.close();
        }
        catch(Exception e){
            Log.e("ERROR", "ERROR: Failed to query the document table: " + e.getStackTrace());
        }
        return result;
    }

    /**
     *
     * @return List with all the documents in the database
     */
    public ArrayList<Document> getDocuments(){
        ArrayList<Document> result = new ArrayList<Document>();
        try{
            //Open the database
            DatabaseHelper databaseHelper = DatabaseHelper.getHelper(context);
            SQLiteDatabase db = databaseHelper.getWritableDatabase();

            //Perform the query and store it in an Cursor object
            String select = "SELECT * FROM document";

            Cursor cursor = db.rawQuery(select,null);

            if(cursor.moveToFirst()){
                do{
                    try{
                        SimpleDateFormat sdf_ymd = new SimpleDateFormat("yyMMdd");
                        Date birthDate = sdf_ymd.parse(cursor.getString(1)); //Position 1: Birth Date
                        Date expirationDate = sdf_ymd.parse(cursor.getString(2)); //Position 2: Expiration Date
                        Document document = new Document(cursor.getString(0), birthDate,
                                expirationDate, cursor.getBlob(3), cursor.getBlob(4), context);
                        result.add(document);
                    } catch (ParseException e) {
                        Log.e("ERROR", "ERROR: Failed to parse the date: " + e.getStackTrace());
                    }
                } while(cursor.moveToNext());
            }
            //Close Cursor object
            cursor.close();

            //Close database
            db.close();
        }
        catch(Exception e){
            Log.e("ERROR", "ERROR: Failed to query the document table: " + e.getStackTrace());
        }
        return result;
    }

    public Document getDocument(String documentNumber){
        Document doc = null;

        //Open the database
        DatabaseHelper databaseHelper = DatabaseHelper.getHelper(context);
        SQLiteDatabase db = databaseHelper.getWritableDatabase();

        //Perform the query and store it in an Cursor object
        String select = "SELECT * FROM document WHERE " + DatabaseHelper.DOCUMENT_NUMBER
                + "= \"" + documentNumber +"\"";

        Cursor cursor = db.rawQuery(select,null);

        if(cursor.moveToFirst()){
            do{
                try{
                    SimpleDateFormat sdf_ymd = new SimpleDateFormat("yyMMdd");
                    Date birthDate = sdf_ymd.parse(cursor.getString(1)); //Position 1: Birth Date
                    Date expirationDate = sdf_ymd.parse(cursor.getString(2)); //Position 2: Expiration Date
                    Document document = new Document(cursor.getString(0), birthDate,
                            expirationDate, cursor.getBlob(3), cursor.getBlob(4), context);
                    doc = document;
                } catch (ParseException e) {
                    Log.e("ERROR", "ERROR: Failed to parse the date: " + e.getStackTrace());
                } catch (java.text.ParseException e) {
                    e.printStackTrace();
                }
            } while(cursor.moveToNext());
        }
        //Close Cursor object
        cursor.close();

        //Close database
        db.close();

        return doc;
    }
}
