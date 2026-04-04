package com.fufufu.executor;

import android.Manifest;
import android.app.Activity;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.ColorDrawable;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.Icon;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.app.PendingIntent;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;

public class LauncherActivity extends Activity {
	private String internalKey;
	private String FILES_PATH;
	private String ROOT_DIR;
	private static final String TAG = "TerminalSetup";
	private ArrayList<HashMap<String, Object>> lmShList = new ArrayList<>();
	
	private LinearLayout lMain;
	private FrameLayout term_layout;
	private TextView tv_empty;
	private GridView lvScript;
	private TextView tTitle;
	private android.graphics.Typeface monoFont;
	private TerminalHelper termHelper;
	
	@Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		monoFont = android.graphics.Typeface.createFromAsset(getAssets(), ExecutorString.FONT);
		ROOT_DIR = android.os.Environment.getExternalStorageDirectory().getAbsolutePath() + "/";
		FILES_PATH = getFilesDir().getPath() + "/";
		requestWindowFeature(Window.FEATURE_NO_TITLE);
		
		setContentView(R.layout.launcher);
		InitTerminalDirectories();
		getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
			getWindow().setClipToOutline(true);
		}
		
		WindowManager.LayoutParams params = getWindow().getAttributes();
		DisplayMetrics displayMetrics = new DisplayMetrics();
		getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);
		params.width = (int) (displayMetrics.widthPixels * 0.9);
		params.height = ViewGroup.LayoutParams.WRAP_CONTENT;
		params.gravity = Gravity.CENTER;
		
		getWindow().clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND);
		params.dimAmount = 0.0f; 
		
		getWindow().setAttributes(params);
		initialize(_savedInstanceState);
		termHelper = new TerminalHelper(this);
		if (getIntent().hasExtra("shortcut_path")) {
			String path = getIntent().getStringExtra("shortcut_path");
			_runSh(path);
		}
		if (Build.VERSION.SDK_INT >= 23) {
			if (checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED
			||checkSelfPermission(Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
				requestPermissions(new String[] {Manifest.permission.READ_EXTERNAL_STORAGE, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1000);
			} else {
				initializeLogic();
			}
		} else {
			initializeLogic();
		}
	}
	
	@Override
	public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if (requestCode == 1000) {
			initializeLogic();
		}
	}
	
	private void initialize(Bundle _savedInstanceState) {
		term_layout = findViewById(R.id.term_layout);
		lMain = findViewById(R.id.lMain);
		tv_empty = findViewById(R.id.tv_empty);
		lvScript = findViewById(R.id.lvScript);
		tTitle = findViewById(R.id.tTitle);
		lvScript.setVerticalScrollBarEnabled(false);
		lvScript.setNumColumns((int)2);
	}
	
	private void initializeLogic() {
		term_layout.setVisibility(View.GONE);
		float density = getResources().getDisplayMetrics().density;
		int radiusPx = (int) (20 * density);
		int strokePx = (int) (2 * density);
		
		android.graphics.drawable.GradientDrawable gd = new android.graphics.drawable.GradientDrawable();
		gd.setColor(0xFF000000); 
		
		gd.setCornerRadius(radiusPx);
		gd.setStroke(strokePx, 0xFF00FF00);
		
		if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.JELLY_BEAN) {
			lMain.setBackground(gd);
		} else {
			lMain.setBackgroundDrawable(gd);
		}
		
		tTitle.setTypeface(monoFont);
		tv_empty.setTypeface(monoFont);
		_loadInternalSh();
		internalKey = ExeConfig.initSync(this, "Run Script");
		loadDataFromServer();
	}
	
	@Override
	public void onResume() {
		super.onResume();
		_loadInternalSh();
	}
	
	
	public void _loadInternalSh() {
		tv_empty.setVisibility(View.GONE);
		lvScript.setVisibility(View.GONE);
		lmShList.clear();
		java.io.File folder = new java.io.File(ROOT_DIR + "Executor/");
		java.io.File[] files = folder.listFiles();
		
		if (files != null) {
			for (java.io.File file : files) {
				if (file.isFile() && file.getName().toLowerCase().endsWith(".sh")) {
					HashMap<String, Object> _item = new HashMap<>();
					_item.put("NAME", file.getAbsolutePath());
					lmShList.add(_item);
				}
			}
		}
		
		if (!lmShList.isEmpty()) {
			java.util.Collections.sort(lmShList, new java.util.Comparator<HashMap<String, Object>>() {
				@Override
				public int compare(HashMap<String, Object> map1, HashMap<String, Object> map2) {
					return map1.get("NAME").toString().compareToIgnoreCase(map2.get("NAME").toString());
				}
			});
		}
		
		HashMap<String, Object> _dummy = new HashMap<>();
		_dummy.put("NAME", "dummy/OPEN TERMUX");
		lmShList.add(0, _dummy); 
		
		if (lmShList.isEmpty()) {
			tv_empty.setVisibility(View.VISIBLE); 
		}
		_setShList();
	}
	
	public void _runSh(final String scriptPath) {
		termHelper.sendExternalShToTerminal(scriptPath);
	}
	
	public void _setShList() {
		lvScript.setAdapter(new LvScriptAdapter(lmShList));
		((BaseAdapter)lvScript.getAdapter()).notifyDataSetChanged();
		lvScript.setVisibility(View.VISIBLE);
	}
	
	
	public void _openTerminal() {
		String packageName = "com.fufufu.executor";
		String className = "com.fufufu.app.TerminalActivity";
		
		Intent intent = new Intent(Intent.ACTION_MAIN);
		intent.setComponent(new ComponentName(packageName, className));
		intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
		
		try {
			startActivity(intent);
		} catch (Exception e) {
		}
	}
	private void InitTerminalDirectories() {
		try {
			File homeDir = new File(FILES_PATH, "home");
			File configDir = new File(homeDir, ".fufufu");
			File fontFile = new File(configDir, "mono.ttf");
			
			if (!configDir.exists()) {
			}
			if (fontFile.exists()) {
				if (fontFile.length() == 0) {
				}
			} else {
				Etoast("0");
				finish();
			}
			
		} catch (Exception e) {
		}
	}
	
	public void _createShortcut(String path, String label) {
		Intent shortcutIntent = new Intent(this, LauncherActivity.class);
		shortcutIntent.setAction(Intent.ACTION_MAIN);
		shortcutIntent.putExtra("shortcut_path", path);
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
			ShortcutManager shortcutManager = getSystemService(ShortcutManager.class);
			
			if (shortcutManager.isRequestPinShortcutSupported()) {
				ShortcutInfo pinShortcutInfo = new ShortcutInfo.Builder(this, path)
				.setShortLabel(label)
				.setIcon(Icon.createWithResource(this, R.drawable.ic_foreground))
				.setIntent(shortcutIntent)
				.build();
				
				Intent pinnedShortcutCallbackIntent = shortcutManager.createShortcutResultIntent(pinShortcutInfo);
				PendingIntent successCallback = PendingIntent.getBroadcast(this, 0, pinnedShortcutCallbackIntent, PendingIntent.FLAG_IMMUTABLE);
				
				shortcutManager.requestPinShortcut(pinShortcutInfo, successCallback.getIntentSender());
			}
		} else {
			Intent addIntent = new Intent();
			addIntent.putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent);
			addIntent.putExtra(Intent.EXTRA_SHORTCUT_NAME, label);
			addIntent.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, Intent.ShortcutIconResource.fromContext(this, R.drawable.ic_foreground));
			addIntent.setAction("com.android.launcher.action.INSTALL_SHORTCUT");
			sendBroadcast(addIntent);
			Etoast("Shortcut " + label + " dibuat!");
		}
	}
	
	private void Etoast(String pesan) {
		Executor.EToast(this, pesan);
	}
	
	private void loadDataFromServer() {
		if (internalKey == null) {
			tTitle.setText(internalKey);
			ExeConfig.triggerSilentLock(this);
		} else {
			internalKey = "fufufu";
		}
	}
	
	public class LvScriptAdapter extends BaseAdapter {
		
		ArrayList<HashMap<String, Object>> _data;
		
		public LvScriptAdapter(ArrayList<HashMap<String, Object>> _arr) {
			_data = _arr;
		}
		
		@Override
		public int getCount() {
			return _data.size();
		}
		
		@Override
		public HashMap<String, Object> getItem(int _index) {
			return _data.get(_index);
		}
		
		@Override
		public long getItemId(int _index) {
			return _index;
		}
		
		@Override
		public View getView(final int _position, View _v, ViewGroup _container) {
			GenericViewHolder holder;
			View _view = _v; 
			
			if (_view == null) {
				_view = getLayoutInflater().inflate(R.layout.script_listview, _container, false);
				holder = new GenericViewHolder();
				
				holder.lSide = _view.findViewById(R.id.lSide);
				holder.lSideMain = _view.findViewById(R.id.lSideMain);
				holder.lSideExpand = _view.findViewById(R.id.lSideExpand);
				holder.lShadow = _view.findViewById(R.id.lShadow);
				holder.tNumb = _view.findViewById(R.id.tNumb);
				holder.tEnded = _view.findViewById(R.id.tEnded);
				holder.tFolder = _view.findViewById(R.id.tFolder);
				
				_view.setTag(holder);
			} else {
				holder = (GenericViewHolder) _view.getTag();
			}
			
			final LinearLayout lSide = holder.lSide;
			final LinearLayout lSideMain = holder.lSideMain;
			final ExecutorLayout lSideExpand = holder.lSideExpand;
			final LinearLayout lShadow = holder.lShadow;
			final TextView tNumb = holder.tNumb;
			final TextView tEnded = holder.tEnded;
			final TextView tFolder = holder.tFolder;
			
			lShadow.setVisibility(View.GONE);
			lSideExpand.setVisibility(View.GONE);
			
			tFolder.setTypeface(monoFont);
			tNumb.setTypeface(monoFont);
			tEnded.setTypeface(monoFont);
			
			tFolder.setText(Uri.parse(_data.get((int)_position).get("NAME").toString()).getLastPathSegment());
			tNumb.setText("[");
			tNumb.setTextColor(0xFF00FF00);
			tEnded.setText("] ");
			tEnded.setTextColor(0xFF00FF00);
			
			lSideMain.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View _view) {
					if (_data.get((int)_position).get("NAME").toString().contains("dummy")) {
						_openTerminal();
					} else {
						_runSh(_data.get((int)_position).get("NAME").toString());
					}
				}
			});
			lSideMain.setOnLongClickListener(new View.OnLongClickListener() {
				@Override
				public boolean onLongClick(View _view) {
					final String fullPath = _data.get((int)_position).get("NAME").toString();
					if (!fullPath.contains("dummy")) {
						
						android.widget.PopupMenu popup = new android.widget.PopupMenu(LauncherActivity.this, _view);
						
						popup.getMenu().add(android.view.Menu.NONE, 1, 1, "Shortcut");
						
						popup.setOnMenuItemClickListener(new android.widget.PopupMenu.OnMenuItemClickListener() {
							@Override
							public boolean onMenuItemClick(android.view.MenuItem item) {
								if (item.getItemId() == 1) {
									String fileName = Uri.parse(fullPath).getLastPathSegment();
									_createShortcut(fullPath, fileName);
									return true;
								}
								return false;
							}
						});
						
						popup.show();
					}
					return true; 
				}
			});
			return _view;
		}
	}

	static class GenericViewHolder {
		LinearLayout lSide, lSideMain, lShadow;
		ExecutorLayout lSideExpand;
		TextView tNumb, tEnded, tFolder;
	}
}
