package com.dsmile.cowoview;

import android.content.Context;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.os.AsyncTask;
import java.net.URL;

import android.database.Cursor;
import android.widget.ExpandableListView;

import java.util.HashMap;
import android.widget.Toast;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import android.util.Log;

import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;

import android.view.View;
import android.widget.ExpandableListView.OnChildClickListener;
import android.content.Intent;

public class MainActivity extends ActionBarActivity implements LoaderManager.LoaderCallbacks <Cursor> {
    WorkersDbAdapter dbHelper;
    WorkersSimpleCursorTreeAdapter mAdapter;

    private static final String TAG = "MMAMM";
    private static final String LOG_TAG = "ADAA";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // БД
        dbHelper = new WorkersDbAdapter(this);
        dbHelper.open();

        // если база данных пуста, загружаем данные после запуска
        if (dbHelper.getSpecialtyData().getCount() == 0) {
            UpdateDb();
        }

        ExpandableListView expandableContactListView = (ExpandableListView) findViewById(R.id.workersList);
        mAdapter = new WorkersSimpleCursorTreeAdapter(this,
                R.layout.list_group,
                R.layout.list_child,
                new String[] {WorkersDbAdapter.SPECIALTY_NAME },
                new int[] { R.id.lblListHeader },
                // разбор данных происходит вручную
/*                new String[] { WorkersDbAdapter.F_NAME, WorkersDbAdapter.L_NAME,
                               WorkersDbAdapter.AGE },
                new int[] { R.id.lblListChildName, R.id.lblListChildLastName, R.id.lblListAge}*/null, null);

        expandableContactListView.setAdapter(mAdapter);
        expandableContactListView.setOnChildClickListener(myListItemClicked);

        Loader<Cursor> loader = getSupportLoaderManager().getLoader(-1);
        if (loader != null && !loader.isReset()) {
            getSupportLoaderManager().restartLoader(-1, null, this);
        } else {
            getSupportLoaderManager().initLoader(-1, null, this);
        }
    }

    //our child listener
    private OnChildClickListener myListItemClicked =  new OnChildClickListener() {
        public boolean onChildClick(ExpandableListView parent, View v,
                                    int groupPosition, int childPosition, long id) {

            Cursor cursor = mAdapter.getChild(groupPosition, childPosition);
            int workerId = cursor.getInt(0);

            Cursor specs = dbHelper.getWorkerSpecialities(workerId);
            Log.w("DDATA", "Spec_cnt = " + specs.getCount());
            String specString = "";
            while (specs.moveToNext()) {
                specString += specs.getString(0) + " ";
            }

            Intent newActivity = new Intent(MainActivity.this, DetailsActivity.class);
            newActivity.putExtra("name", cursor.getString(2) + " " + cursor.getString(1));
            newActivity.putExtra("birthday", cursor.getString(3));
            newActivity.putExtra("avatar_url", cursor.getString(4));
            newActivity.putExtra("specs", specString);
            startActivity(newActivity);

            return false;
        }

    };

    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        if (id != -1) {
            return new MyChildCursorLoader(this, dbHelper, id);
        } else {
            return new MyGroupCursorLoader(this, dbHelper);
        }
    }

    public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
        // Setting the new cursor onLoadFinished. (Old cursor would be closed
        // automatically)
        int id = loader.getId();
        Log.d(LOG_TAG, "onLoadFinished() for loader_id " + id);
        if (id != -1) {
            // child cursor
            if (!data.isClosed()) {
                Log.d(LOG_TAG, "data.getCount() " + data.getCount());

                HashMap<Integer, Integer> groupMap = mAdapter.getGroupMap();
                try {
                    int groupPos = groupMap.get(id);
                    Log.d(LOG_TAG, "onLoadFinished() for groupPos " + groupPos);
                    mAdapter.setChildrenCursor(groupPos, data);
                } catch (NullPointerException e) {
                    Log.w(LOG_TAG,
                            "Adapter expired, try again on the next query: "
                                    + e.getMessage());
                }
            }
        } else {
            mAdapter.setGroupCursor(data);
        }

    }

    public void onLoaderReset(Loader<Cursor> loader) {
        // Called just before the cursor is about to be closed.
        int id = loader.getId();
        Log.d(LOG_TAG, "onLoaderReset() for loader_id " + id);
        if (id != -1) {
            // child cursor
            try {
                mAdapter.setChildrenCursor(id, null);
            } catch (NullPointerException e) {
                Log.w(LOG_TAG, "Adapter expired, try again on the next query: "
                        + e.getMessage());
            }
        } else {
            mAdapter.setGroupCursor(null);
        }
    }

   static class MyGroupCursorLoader extends CursorLoader {
        WorkersDbAdapter db;
        public MyGroupCursorLoader(Context context, WorkersDbAdapter db) {
            super(context);
            this.db = db;
        }

        @Override
        public Cursor loadInBackground() {
            return db.getSpecialtyData();
        }
    }


    static class MyChildCursorLoader extends CursorLoader {
        WorkersDbAdapter db;
        int specId;

        public MyChildCursorLoader(Context context, WorkersDbAdapter db, int specId) {
            super(context);
            this.db = db;
            this.specId = specId;
        }

        @Override
        public Cursor loadInBackground() {
            return db.getWorkersData(specId);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_update) {
            UpdateDb();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    public void UpdateDb() {
        dbHelper.ClearTables();
        new UpdateDatabase().execute("http://65apps.com/images/testTask.json");
    }

    private class UpdateDatabase extends AsyncTask<String, Long, Long> {
        private String Content = null;
        private String Error = null;

        protected void onPreExecute() {
            //Start Progress Dialog (Message)
            Toast.makeText(MainActivity.this, R.string.update_db_start, Toast.LENGTH_SHORT).show();
        }

        protected Long doInBackground(String... urls) {
            BufferedReader reader=null;
            // Send data
            try {
                // Defined URL  where to send data
                URL url = new URL(urls[0]);
                Log.w(TAG, url.toString());

                HttpURLConnection conn = (HttpURLConnection)url.openConnection();
                Log.w(TAG, "Conn opened");
                conn.setConnectTimeout(10000);
                conn.setReadTimeout(10000);
                conn.setInstanceFollowRedirects(true);
                conn.setRequestMethod("GET");
                conn.setDoInput(true);
                conn.connect();

                Log.w(TAG, "Conn opened");
                // Get the server response
                reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
                StringBuilder sb = new StringBuilder();
                String line;

                Log.w(TAG, "Start read");
                // Read Server Response
                while((line = reader.readLine()) != null)
                {
                    // Append server response in string
                    sb.append(line);
                    // Escape early if cancel() is called
                    if (isCancelled()) break;
                }
                // Append Server Response To Content String
                if (!isCancelled()) {
                    Content = sb.toString();
                }
            }
            catch(Exception ex) {
                Error = ex.getMessage();

            }
            finally {
                try {
                    reader.close();
                } catch(Exception ex) {
                    // TODO
                }
            }
            return 0l;
        }

        protected void onPostExecute(Long result) {
            if (Error != null) {
                Toast.makeText(MainActivity.this, R.string.update_db_error, Toast.LENGTH_SHORT).show();
                Log.w(TAG, "Output: "+Error);
            } else {
                JSONObject jsonResponse;
                try {
                    jsonResponse = new JSONObject(Content);
                    JSONArray jsonMainNode = jsonResponse.optJSONArray("response");

                    int lengthJsonArr = jsonMainNode.length();
                    for (int i=0; i < lengthJsonArr; i++) {
                        JSONObject jsonChildNode = jsonMainNode.getJSONObject(i);

                        String f_name = DataConditioning.properCase(jsonChildNode.optString("f_name"));
                        String l_name = DataConditioning.properCase(jsonChildNode.optString("l_name"));
                        String birthday = DataConditioning.properDate(jsonChildNode.optString("birthday"));
                        String avatr_url = jsonChildNode.optString("avatr_url");

                        long worker_id = dbHelper.createWorker(f_name, l_name, birthday, avatr_url, 0);

                        // Разбор специальностей
                        JSONArray jsonSpecs = jsonChildNode.getJSONArray("specialty");
                        if (worker_id != -1) {
                            for (int j = 0; j < jsonSpecs.length(); j++) {
                                JSONObject jsonSpecItem = jsonSpecs.getJSONObject(j);
                                int specialty_id = jsonSpecItem.getInt("specialty_id");
                                // Если не будет названия специальности, вызвать исключение
                                String specName = jsonSpecItem.getString("name");
                                Log.w(TAG, "Spec: " + String.valueOf(specialty_id) + " - " + specName + ">" + worker_id);
                                dbHelper.createSpeciality(specialty_id, specName, worker_id);
                            }
                        }
                    }
                    Toast.makeText(MainActivity.this, R.string.update_db_end, Toast.LENGTH_SHORT).show();
                    Loader<Cursor> loader = getSupportLoaderManager().getLoader(-1);
                    if (loader != null && !loader.isReset()) {
                        getSupportLoaderManager().restartLoader(-1, null, MainActivity.this);
                    } else {
                        getSupportLoaderManager().initLoader(-1, null, MainActivity.this);
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }

        protected void onCancelled (Long result) {
        // TODO
        }
    }

    protected void onDestroy() {
        super.onDestroy();
        dbHelper.close();
    }
}
