package passportreader.retrovanilla.com.androidpassportreader;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.nfc.NfcAdapter;
import android.nfc.Tag;
import android.nfc.tech.IsoDep;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.util.Log;
import android.view.ContextMenu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import net.sourceforge.scuba.smartcards.CardService;
import net.sourceforge.scuba.smartcards.CardServiceException;

import org.jmrtd.BACKeySpec;
import org.jmrtd.PassportService;
import org.jmrtd.Util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Vector;

import javax.crypto.SecretKey;

import passportreader.retrovanilla.com.androidpassportreader.data.Document;
import passportreader.retrovanilla.com.androidpassportreader.database.DatabaseHandler;
import passportreader.retrovanilla.com.androidpassportreader.jmrtd.OwnPassportService;

public class CheckPassportActivity extends AppCompatActivity {

    private static final int CHALLENGE = 1;
    private static final int REPLAY = 2;
    private static final int WRONGDATA = 3;
    private static final int REPLAY_TIMING = 4;
    private static final int WRONGDATA_TIMING = 5;
    private static final int BAC = 6;
    private static final int TIMING_ATTACK_RETRIES = 10;
    private int attack_retries;
    private TextView tv;
    private TextView ttime;
    private Tag tag;
    private Document doc;
    private SharedPreferences prefs;
    private DatabaseHandler handler;
    private String macString = "";
    private String totalTime = "";
    private ArrayList<byte[]> responses;
    private int action;
    boolean challenged = false;
    private double totalTiming;
    private int timingCount;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_check_passport);

        tv = (TextView) findViewById(R.id.tag_output);
        ttime = (TextView) findViewById(R.id.total_time);

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
        //this.getSharedPreferences(
        //"passportreader.retrovanilla.com.androidpassportreader", Context.MODE_PRIVATE);
        handler = new DatabaseHandler(getApplicationContext());
        responses = new ArrayList<byte[]>();
        timingCount = 0;
        attack_retries = Integer.parseInt(prefs.getString("number_of_attacks",""+TIMING_ATTACK_RETRIES));
        Log.v("ANTO","Retries: "+attack_retries);
        registerForContextMenu(tv);
        checkIntent(getIntent());
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {

        if(action == CHALLENGE){
            String msg = "Could not export.";
            if(writeFile()) {
                msg = "Saved: (Download Dir)/PassportReader/tagresponses";

            }
            Snackbar snackbar =
                    Snackbar.make(tv, msg, Snackbar.LENGTH_SHORT);
            snackbar.show();

            return;
        }
        TextView textView = (TextView) v;

        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        ClipData data = ClipData.newPlainText("RandomValues", textView.getText());
        clipboard.setPrimaryClip(data);
        Snackbar snackbar =
                Snackbar.make(tv, "Copied to clipboard", Snackbar.LENGTH_SHORT);
        snackbar.show();
    }

    private boolean writeFile(){
        boolean ok = false;
        FileOutputStream fos = null;
        try {
            File root = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
            File dir = new File (root + "/PassportReader");
            dir.mkdirs();
            File file = new File(dir, "/tagresponses");
            file.createNewFile();
            fos = new FileOutputStream(file);
            for (byte[] resp : responses ) {
                fos.write(resp);
            }
            ok = true;
        } catch (IOException e) {
            ok = false;
            e.printStackTrace();
        }finally{
            try {
                if(fos!=null)
                    fos.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
            return ok;
        }

    }

    private void checkIntent(Intent intent){
        String documentNumber = prefs.getString("passport.selected", "-1");

        doc = handler.getDocument(documentNumber);

        if(doc != null){
            Button tview = (Button) findViewById(R.id.replay_btn);
            if (doc.getMac() == null)
                tview.setEnabled(false);
            else
                tview.setEnabled(true);
        }else {
            Snackbar snackbar =
                    Snackbar.make(tv, "No docucment", Snackbar.LENGTH_SHORT);
            snackbar.show();
        }

        if(intent.getAction().equals(NfcAdapter.ACTION_TECH_DISCOVERED)){
            if(intent.getParcelableExtra(NfcAdapter.EXTRA_TAG)!=null){
                tag = intent.getParcelableExtra(NfcAdapter.EXTRA_TAG);
            }

        }else{
            tv.setText(getString(R.string.unable_to_process) +" " + intent.getAction());
        }

    }

    @Override
    protected void onNewIntent(Intent intent) {
        checkIntent(intent);
        super.onNewIntent(intent);
    }

    public void challenge(View view){
        action = CHALLENGE;

        if(!challenged)
            tv.setText("Tag output...\n");
        challenged = true;
        if(tag== null) {
            Snackbar snackbar =
                    Snackbar.make(tv, "Tag Error", Snackbar.LENGTH_SHORT);
            snackbar.show();
        }
        IsoDep dep = IsoDep.get(tag);
        try{
            dep.setTimeout(1000);
            new challengePassport().execute(dep);

        }catch(Exception e){
            Snackbar snackbar =
                    Snackbar.make(tv, "Challenge Error", Snackbar.LENGTH_SHORT);
            snackbar.show();
        }
    }

    public void authenticate(View view){
        action = BAC;
        challenged = false;
        tv.setText("Tag output...\n");
        ttime.setText("Total time: ");
        passportComm();
    }

    public void replay(View view){
        challenged = false;
        action = REPLAY;
        tv.setText("Tag Output...\n");
        ttime.setText("Total time: ");
        passportComm();
    }

    public void sendWrong(View view){
        challenged = false;
        action = WRONGDATA;
        tv.setText("Tag Output...\n");
        ttime.setText("Total time: ");
        passportComm();
    }

    public void replayTiming(View view){
        totalTiming = 0;
        timingCount = 0;
        tv.setText("Tag Output...\n");
        ttime.setText("Total time: ");
        replayAttack(REPLAY_TIMING);
    }

    public void wrongDataTiming(View view){
        totalTiming = 0;
        timingCount = 0;
        tv.setText("Tag Output...\n");
        ttime.setText("Total time: ");
        replayAttack(WRONGDATA_TIMING);
    }

    public void replayAttack(int replay_action){
        challenged = false;
        action = replay_action;
        passportComm();
    }

    public void passportComm(){
        IsoDep dep = IsoDep.get(tag);
        try{
            dep.setTimeout(1000);
            new PassportAuthentication().execute(dep);

        }catch(Exception e){
            Snackbar snackbar =
                    Snackbar.make(tv, "Communication Error", Snackbar.LENGTH_SHORT);
            snackbar.show();
        }
    }



    /** Challenge Passport AsyncTask
     *
     */
    class challengePassport extends AsyncTask<IsoDep, String, Boolean> {

        StringBuilder randomValues = new StringBuilder();
        CardService cService;
        @Override
        protected Boolean doInBackground(IsoDep... params) {
            IsoDep iso=params[0];
            Boolean ok = false;
            //cardService instance for nfc communication
            cService=CardService.getInstance(iso);
            try {
                cService.open();//begin new session
                PassportService pService =new PassportService(cService);
                for(int ii = 0; ii < 100 ; ii++) {

                    BigInteger responseBI;
                    String respString;
                    byte[] p_rndICC =  new byte[9];
                    p_rndICC[0] = 0;
                    byte[] resp = pService.sendGetChallenge();
                    responses.add(resp);
                    System.arraycopy(resp, 0, p_rndICC, 1, 8);
                    responseBI = new BigInteger(p_rndICC);
                    respString = responseBI.toString();
                    randomValues.append("\n").append(respString);
                }

            } catch (CardServiceException e) {
                e.printStackTrace();
                return false;
            }

            return true;
        }


        @Override
        protected void onPostExecute(Boolean output) {
            if (output == true) {
                tv.append(randomValues);
                if(cService != null && cService.isOpen())
                    cService.close();
            } else {
                Snackbar snackbar =
                        Snackbar.make(tv, "Passport not present", Snackbar.LENGTH_SHORT);
                snackbar.show();
            }
        }
    }
    /** End Challenge Passport AsyncTask */

    /** Authenticate Passport AsyncTask
     *
     */
    class PassportAuthentication extends AsyncTask<IsoDep, String, String> {

        @Override
        protected String doInBackground(IsoDep... params) {
            String response = "";
            IsoDep iso=params[0];
            CardService cService = CardService.getInstance(iso);
            OwnPassportService pService = null;
            try {
                if(!cService.isOpen())
                    cService.open();//begin new session

                if(action == REPLAY || action == REPLAY_TIMING)
                    pService = new OwnPassportService(cService, true, doc.getMac(), doc.getMessage());
                else
                    pService = new OwnPassportService(cService, false, doc.getMac(), doc.getMessage());

                Vector<BACKeySpec> bacs = new Vector<BACKeySpec>();
                bacs.add(doc);

                pService.open();
                pService.setDoc(doc);

                String maxDocumentNumber = "000000000";
                if(action != WRONGDATA && action != WRONGDATA_TIMING) {
                    String minDocumentNumber = doc.getDocumentNumber().replace('<', ' ').trim().replace(' ', '<');
                    for (maxDocumentNumber = minDocumentNumber;
                         maxDocumentNumber.length() < 9;
                         maxDocumentNumber = maxDocumentNumber + "<") {
                        ;
                    }
                }
                if(action != REPLAY_TIMING && action != WRONGDATA_TIMING || timingCount == 0)
                    response = "<br/>Gonna try BAC with: "+maxDocumentNumber+" - "+doc.getDateOfBirth()+" - "+doc.getDateOfExpiry();
                else
                    response = "";
                byte[] cse = Util.computeKeySeed(maxDocumentNumber, doc.getDateOfBirth(), doc.getDateOfExpiry());
                SecretKey kEnc = Util.deriveKey(cse, 1);
                SecretKey kMac = Util.deriveKey(cse, 2);

                pService.sendSelectApplet();

                pService.doBAC(kEnc, kMac);

                response += "<div align='center'><font color='#348017'><b>Communication OK!</b></font></div>";
            } catch (CardServiceException e1) {
                if(action == REPLAY_TIMING || action == WRONGDATA_TIMING){
                    if(timingCount < attack_retries){
                        totalTiming += pService.getTotalTime();
                        replayAttack(action);
                    }else{
                        response = "<br/><br/>Replay attack ended!\n";
                    }
                }else
                    response += "\n\nCardService Error! " + e1.getMessage();
            } catch (GeneralSecurityException e1) {
                response += "\n\nGeneral Security Exception! " + e1.getMessage();
            }finally{
                cService.close();
                if(pService!=null) {
                    if(action == REPLAY_TIMING || action == WRONGDATA_TIMING){
                        if(timingCount < attack_retries){
                            timingCount++;
                            response += "<br/>Attack #" + timingCount
                                    + " time: " + pService.getTotalTime() +" ms.";
                        }else {
                            totalTiming = totalTiming / attack_retries;
                            response += "<br/><font color='#348017'><b>AVG RESPONSE TIME: "
                                    + totalTiming + " ms.</b></font>";
                            totalTime = "Total time: " + totalTiming + " ms.";
                        }
                    }else{
                        response += "\n" + pService.getOutput();
                        totalTime = "Total time: " + pService.getTotalTime() + " ms.";
                    }

                }
            }
            return response;
        }

        @Override
        protected void onPostExecute(String response) {
            if (response == null) {
                Snackbar snackbar =
                        Snackbar.make(tv, "There was no communication", Snackbar.LENGTH_SHORT);
                snackbar.show();
                Intent i=new Intent(getApplicationContext(), MainActivity.class);
                startActivity(i);
            } else {
                tv.append(Html.fromHtml(response));
                ttime.setText(totalTime);
            }
        }
    }

}
