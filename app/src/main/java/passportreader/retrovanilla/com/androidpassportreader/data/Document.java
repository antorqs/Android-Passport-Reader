package passportreader.retrovanilla.com.androidpassportreader.data;

import android.content.Context;

import org.jmrtd.BACKey;

import java.io.Serializable;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

import passportreader.retrovanilla.com.androidpassportreader.database.DatabaseHandler;

/**
 * Created by antonio on 16/12/15.
 */
public class Document extends BACKey implements Serializable{
    SimpleDateFormat sdf_dmy = new SimpleDateFormat("dd/MM/yy");
    SimpleDateFormat sdf_ymd = new SimpleDateFormat("yyMMdd");
    private byte[] mac;
    private byte[] message;
    private Context context;

    public Document(String documentNumber, Date birthDate, Date expirationDate) {
        this(documentNumber, birthDate, expirationDate, null, null, null);
    }

    public Document(String documentNumber, Date birthDate, Date expirationDate,
                    byte[] mac, byte[] message, Context ctx) {
        super(documentNumber, birthDate, expirationDate);
        this.mac = mac;
        this.context = ctx;
        this.message = message;
    }

    public Document(String documentNumber, String birthDate, String expirationDate) {
        this(documentNumber, birthDate, expirationDate, null, null, null);
    }

    public Document(String documentNumber, String birthDate, String expirationDate,
                    byte[] mac, byte[] message, Context ctx) {
        super(documentNumber, birthDate, expirationDate);
        this.mac = mac;
        this.context = ctx;
        this.message = message;
    }

    public Date getExpirationDate(){
        try {
            return sdf_ymd.parse(getDateOfExpiry());
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return null;
    }


    public String getExpirationDateString() {
        String parsedDate="";
        try{
            String date = getDateOfExpiry();
            Date eDate = sdf_ymd.parse(date);
            parsedDate = sdf_dmy.format(eDate);
        }catch(Exception e){
            e.printStackTrace();
        }

        return parsedDate;
    }


    public Date getBirthDate(){

        try {
            return sdf_ymd.parse(getDateOfBirth());
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return null;
    }

    public String getBirthDateString(){
        String parsedDate="";
        try{
            String date=getDateOfBirth();
            Date eDate = sdf_ymd.parse(date);
            parsedDate = sdf_dmy.format(eDate);
        }catch(Exception e){
            e.printStackTrace();
        }

        return parsedDate;
    }


    public byte[] getMac() {
        return mac;
    }

    public void setMac(byte[] mac) {
        this.mac = mac;
    }

    public byte[] getMessage() {
        return message;
    }

    public void setMessage(byte[] message) {
        this.message = message;
    }

    public void update(){
        DatabaseHandler handler = new DatabaseHandler(context);
        handler.updateDocument(this);
    }
}
