package passportreader.retrovanilla.com.androidpassportreader;

import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.nfc.NfcAdapter;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.design.widget.FloatingActionButton;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;

import passportreader.retrovanilla.com.androidpassportreader.data.Document;
import passportreader.retrovanilla.com.androidpassportreader.data.DocumentArrayAdapter;
import passportreader.retrovanilla.com.androidpassportreader.database.DatabaseHandler;

public class MainActivity extends AppCompatActivity {

    private ListView documentList; //BAC listview for the adapter
    public static DocumentArrayAdapter adapter; //BAC listview's custom adapter
    private DatabaseHandler handler; //DatabaseHandler instance
    private View selected;
    private NfcAdapter NFCadapter;
    private int selectedPos;
    SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getApplicationContext(), EditDocumentActivity.class);
                startActivity(intent);
            }
        });

        prefs = PreferenceManager.getDefaultSharedPreferences(this);
                //this.getSharedPreferences(
                //"passportreader.retrovanilla.com.androidpassportreader", Context.MODE_PRIVATE);
        handler = new DatabaseHandler(getApplicationContext());

        documentList = (ListView)findViewById(R.id.main_documentList);

        adapter = new DocumentArrayAdapter(getApplicationContext(),
                R.layout.document_layout,
                handler.getDocuments());
        documentList.setAdapter(adapter);

        documentList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                markSelected(position, view);
                Document doc = adapter.getDocument(selectedPos);
                prefs.edit().putString("passport.selected", doc.getDocumentNumber()).apply();

            }
        });

        final Handler handl = new Handler();
        handl.postDelayed(new Runnable() {
            @Override
            public void run() {
                selectedPos = 0;
                documentList.setSelection(0);
                selected = documentList.getChildAt(0);
                if(selected != null)
                    selected.setBackgroundResource(R.color.lightBlue);
            }
        }, 500);
        registerForContextMenu(documentList);

    }

    private void markSelected(int position, View view){
        selectedPos = position;
        if (selected != null) {
            selected.setBackgroundResource(R.color.lightGray);
        }
        selected = view;
        view.setBackgroundResource(R.color.lightBlue);
        resetForegroundDispatch();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            Intent i = new Intent(this, SettingsActivity.class);
            startActivity(i);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onResume() {
        enableForegroundDispatch();

        //refresh the list
        adapter.populateList(handler.getDocuments());
        adapter.notifyDataSetChanged();

        super.onResume();
    }

    @Override
    public void onCreateContextMenu(ContextMenu menu,
                                    View v,
                                    ContextMenu.ContextMenuInfo menuInfo){

        super.onCreateContextMenu(menu, v, menuInfo);

        MenuInflater mInflater = getMenuInflater();
        mInflater.inflate(R.menu.context_menu, menu);

        switch (v.getId()){

            case R.id.main_documentList:
                menu.setHeaderTitle("Document Options");
                break;

            default:
                break;
        }
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {

        AdapterView.AdapterContextMenuInfo mInfo =
                (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
        Document document = adapter.getItem(mInfo.position);
        switch(item.getItemId()){
            case R.id.ctxMenu_Modify:

                Intent mIntent=new Intent(getApplicationContext(), EditDocumentActivity.class);
                mIntent.putExtra("document", document);
                startActivity(mIntent);
                break;

            case R.id.ctxMenu_delete:
                handler.deleteDocument(document);
                adapter.remove(document);
                break;

            default:
                break;
        }
        return super.onContextItemSelected(item);
    }

    @Override
    public void onContextMenuClosed(Menu menu) {
        adapter.notifyDataSetChanged();
        super.onContextMenuClosed(menu);
    }

    private void resetForegroundDispatch(){
        disableForegroundDispatch();
        enableForegroundDispatch();
    }

    private void enableForegroundDispatch(){
        NFCadapter = NfcAdapter.getDefaultAdapter(this); //get default nfc adapter

        String[][] techs = new String[][] { new String[] { "android.nfc.tech.IsoDep" } }; //the technology we are interested in
        Intent intent = new Intent(getApplicationContext(), CheckPassportActivity.class); //prepare the intent to the reader activity
        intent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);

        PendingIntent pending =
                PendingIntent.getActivity(this, 0, intent,
                        PendingIntent.FLAG_UPDATE_CURRENT);

        //enable the foregroundDispatch,
        //this will give this PassportInfoDisplay priority over another
        //to manage this intent
        NFCadapter.enableForegroundDispatch(this, pending, null, techs);
    }

    private void disableForegroundDispatch(){
        try{
            NFCadapter.disableForegroundDispatch(this);
        }catch(Exception e){
            e.printStackTrace();
        }
    }

    @Override
    protected void onPause() {
        disableForegroundDispatch();
        super.onPause();
    }
}
