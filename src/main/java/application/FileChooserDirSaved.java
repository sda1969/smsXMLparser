package application;

import java.io.File;
import java.util.prefs.Preferences;
import javafx.stage.FileChooser;
import javafx.stage.Window;

// храним последний открытый каталог из любого экземпляра класса
@SuppressWarnings("serial")
public class FileChooserDirSaved {
	
	public static Preferences localPrefs; 
	private FileChooser fileChooser;
	public static final String id = "dirSms";
	
	public FileChooserDirSaved(String title, String  filterTitle, String filterStr, Preferences prefs) {
		localPrefs = prefs;
		fileChooser = new FileChooser();
		fileChooser.setTitle(title);
		 // Set extension filter
        FileChooser.ExtensionFilter extFilter = 
                new FileChooser.ExtensionFilter(filterTitle, filterStr);
        fileChooser.getExtensionFilters().add(extFilter);
	}
	
	public File showOpenDialog(Window w) {
		fileChooser.setInitialDirectory(getDir());
		File f = null;
		try {
			f = fileChooser.showOpenDialog(w);
		}
		catch (IllegalArgumentException e) {
			//возможно был сохранен путь к съемному носителю, тогда корень
			fileChooser.setInitialDirectory( new File("/"));
			f = fileChooser.showOpenDialog(w);
		}
		if (f != null) {
			localPrefs.put(id, f.getParent());
		}
		return f;
	}
	
	private File getDir() {
		String strDir = localPrefs.get(id, "/");
		File fDir = new File(strDir);
		return fDir;
	}
}
