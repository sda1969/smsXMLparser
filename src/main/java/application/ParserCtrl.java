package application;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.net.URL;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.function.Function;
import java.util.prefs.Preferences;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.AnchorPane;

public class ParserCtrl extends AnchorPane implements Initializable {
	// localPrefs только для хранения на локальной машине последнего IP address
	// устройства
	public final static Preferences localPrefs = Preferences.userRoot().node("parseSMS");
	private static final Map<String, String> months = new HashMap<>();
	private static final String regexListFName = "regexList.txt";
	@FXML
	private Label fileNameLbl;
	@FXML
	private ComboBox<String> addrCmb;
	@FXML
	private ComboBox<String> regexCmb;
	@FXML
	private TableView<SMSitem> smsTable;
	@FXML
	private TableColumn<SMSitem, String> dateColumn;
	@FXML
	private TableColumn<SMSitem, String> paramColumn;
	@FXML
	private TableColumn<SMSitem, String> fullColumn;
	@FXML
	private Button loadBtn;
	@FXML
	private Button searchBtn;
	@FXML
	private Button resetBtn;
	@FXML
	private Button saveRegExBtn;
	@FXML
	private Button delRegExBtn;
	@FXML
	private DatePicker fromDateDP;
	@FXML
	private DatePicker toDateDP;

	private File smsFile;
	private List<SMSitem> allSMSlist;
	private double total;
	private BooleanProperty isFileLoaded = new SimpleBooleanProperty(false);
	private LocalDate minDate, maxDate;

	@Override
	public void initialize(URL location, ResourceBundle resources) {
		fileNameLbl.setText("Незагружен");
		ObservableList<String> regexList = loadRegExListFromFile(regexListFName);
		regexCmb.setItems(regexList);
		regexCmb.getSelectionModel().select(0);

		regexCmb.disableProperty().bind(isFileLoaded.not());
		searchBtn.disableProperty().bind(isFileLoaded.not());
		addrCmb.disableProperty().bind(isFileLoaded.not());
		fromDateDP.disableProperty().bind(isFileLoaded.not());
		toDateDP.disableProperty().bind(isFileLoaded.not());
		resetBtn.disableProperty().bind(isFileLoaded.not());
		saveRegExBtn.disableProperty().bind(isFileLoaded.not());
		delRegExBtn.disableProperty().bind(isFileLoaded.not());

		// что-бы можно было выделить фрагмент и скопировать в буфер обмена
		fullColumn.setCellFactory(TextFieldTableCell.<SMSitem>forTableColumn());

		months.put("янв.", "01");
		months.put("февр.", "02");
		months.put("марта", "03");
		months.put("апр.", "04");
		months.put("мая", "05");
		months.put("июня", "06");
		months.put("июля", "07");
		months.put("авг.", "08");
		months.put("сент.", "09");
		months.put("окт.", "10");
		months.put("нояб.", "11");
		months.put("дек.", "12");

		loadBtn.setOnAction(value -> {
			final FileChooserDirSaved fileChooser = new FileChooserDirSaved("Выбор файла СМС архива",
					"файл СМС архива (*.xml)", "*.xml", localPrefs);
			smsFile = fileChooser.showOpenDialog(null);
			fileNameLbl.setText("Незагружен");
			if (smsFile != null) {
				isFileLoaded.setValue(true);
				try {
					fileNameLbl.setText(smsFile.getName());
					allSMSlist = parseSMSfile(smsFile);
					addrCmb.setItems(FXCollections.observableList(getAdressList(allSMSlist)));
					addrCmb.getSelectionModel().select(0);
					minDate = getMinDate(allSMSlist);
					maxDate = getMaxDate(allSMSlist);
					fromDateDP.setValue(minDate);
					toDateDP.setValue(maxDate);
				} catch (Exception e) {
					fileNameLbl.setText(e.getMessage());
					isFileLoaded.setValue(false);
				}
			}
		});

		searchBtn.setOnAction(value -> {
			total = 0;
			String address = addrCmb.getSelectionModel().getSelectedItem();
			String regex = regexCmb.getSelectionModel().getSelectedItem();
			fillTable(address, regex, fromDateDP.getValue(), toDateDP.getValue());
		});

		// какие колонки отображать
		dateColumn.setCellValueFactory(new PropertyValueFactory<>("date"));
		paramColumn.setCellValueFactory(new PropertyValueFactory<>("param"));
		fullColumn.setCellValueFactory(new PropertyValueFactory<>("full"));

		resetBtn.setOnAction(value -> {
			fromDateDP.setValue(minDate);
			toDateDP.setValue(maxDate);
		});

		saveRegExBtn.setOnAction(value -> {
			String regex = regexCmb.getSelectionModel().getSelectedItem();
			ObservableList<String> updRegexList = regexCmb.getItems();
			if (updRegexList.stream().anyMatch(str -> !str.equals(regex))) {
				updRegexList.add(regex);
			}
			regexCmb.setItems(updRegexList);
			saveRegExList2File(regexListFName, updRegexList);
		});

		delRegExBtn.setOnAction(value -> {
			String regex = regexCmb.getSelectionModel().getSelectedItem();
			ObservableList<String> updRegexList = regexCmb.getItems();
			if (!regex.equals(".*")) {
				updRegexList.remove(regex);
				regexCmb.setItems(updRegexList);
				saveRegExList2File(regexListFName, updRegexList);
			}
		});

	}

	private List<String> getAdressList(List<SMSitem> allSMSlist) {
		Set<String> addrSet = new HashSet<>();

		for (SMSitem s : allSMSlist) {
			addrSet.add(getAddress(s));
		}

		return new ArrayList<String>(addrSet);
	}

	private void fillTable(String addr, String regVar, LocalDate from, LocalDate to) {
		ObservableList<SMSitem> list = FXCollections.observableArrayList();
		NumberFormat formatter = new DecimalFormat("#0.00", new DecimalFormatSymbols(Locale.US));

		for (SMSitem s : allSMSlist) {
			if (addr.equalsIgnoreCase(getAddress(s))) {
				LocalDate sdate = LocalDate.parse(s.dateProperty().getValue(),
						DateTimeFormatter.ofPattern("dd.MM.yyyy"));
				if (from.isAfter(sdate) || to.isBefore(sdate))
					continue;

				Pattern pb = Pattern.compile(regVar);
				Matcher mb = pb.matcher(s.fullProperty().getValue());
				if (mb.find()) {
					if (mb.groupCount() > 0) {
						s.paramProperty().setValue(mb.group(1));
						try {
							total += Double.parseDouble(mb.group(1));
						} catch (NumberFormatException e) {
						}
					}
					list.add(s);
				}
			}
		}
		if (total > 0) {
			list.add(new SMSitem("Итого:", formatter.format(total), "ОБШИЙ ИТОГ ЗА ПЕРИОД", "", ""));
		}
		smsTable.setItems(list);
	}

	private List<SMSitem> parseSMSfile(File smsFile) throws Exception {
		List<SMSitem> list = new ArrayList<>();
		try {
			DocumentBuilder documentBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder();
			Document document = documentBuilder.parse(smsFile);
			Node root = document.getDocumentElement();
			NodeList smsList = root.getChildNodes();
			for (int i = 0; i < smsList.getLength(); i++) {
				Node sms = smsList.item(i);
				if (sms.getNodeType() != Node.TEXT_NODE) {
					NamedNodeMap attributes = sms.getAttributes();
					String dateRaw = getAttrTextContent(attributes, "readable_date");
					String body = getAttrTextContent(attributes, "body");
					String address = getAttrTextContent(attributes, "address");
					String contact = getAttrTextContent(attributes, "contact_name");
					list.add(new SMSitem(trimDate(dateRaw), "no", body, address, contact));
				}
			}
		} catch (ParserConfigurationException | SAXException | IOException ex) {
			ex.printStackTrace();
		}

		return list;
	}

	private String getAttrTextContent(NamedNodeMap attributes, String field) throws Exception {
		if (attributes == null) {
			throw new Exception("namedNodeMap is empy");
		}
		if (attributes.getNamedItem(field) == null) {
			throw new Exception("attributes.getNamedItem(field) is null");
		}
		return attributes.getNamedItem(field).getTextContent();
	}

	/*
	 * return date in format dd.mm.yyyy
	 */
	private String trimDate(String str) {
		String date = "хер знает когда";
		String smsDate = str.substring(0, str.indexOf(" г.") + 1);
		Pattern patDate = Pattern.compile("(\\d{1,2})\\s(.*?)\\s(\\d{4}).*");
		Matcher mDate = patDate.matcher(smsDate);
		if (mDate.matches()) {
			if (mDate.groupCount() == 3) {
				String day = mDate.group(1);
				String mm = months.get(mDate.group(2));
				String yy = mDate.group(3);
				if (day.length() < 2)
					day = "0" + day;

				date = day + "." + mm + "." + yy;
			}
		}

		return date;
	}

	/*
	 * что использовать адрес или имя контакта есть имя контакта использовать его,
	 * если нет, тогда использовать адрес
	 */
	private String getAddress(SMSitem s) {
		String address = s.contactProperty().getValue();
		if (address.equals("(Unknown)")) {
			address = s.addressProperty().getValue();
		}
		return address;
	}

	private LocalDate getMinDate(List<SMSitem> allSMSlist) {
		Optional<SMSitem> min = allSMSlist.stream().min((a, b) -> compareSMSdate(a, b));
		LocalDate d;
		if (min.isPresent()) {
			d = LocalDate.parse(min.get().dateProperty().getValue(), DateTimeFormatter.ofPattern("dd.MM.yyyy"));
		} else {
			d = LocalDate.MIN;
		}
		return d;
	}

	private LocalDate getMaxDate(List<SMSitem> allSMSlist) {
		Optional<SMSitem> max = allSMSlist.stream().max((a, b) -> compareSMSdate(a, b));
		LocalDate d;
		if (max.isPresent()) {
			d = LocalDate.parse(max.get().dateProperty().getValue(), DateTimeFormatter.ofPattern("dd.MM.yyyy"));
		} else {
			d = LocalDate.MAX;
		}
		return d;
	}

	private int compareSMSdate(SMSitem i1, SMSitem i2) {

		Function<String, String> convert2ISO = s -> {
			return LocalDate.parse(s, DateTimeFormatter.ofPattern("dd.MM.yyyy")).format(DateTimeFormatter.ISO_DATE);
		};

		String iso1 = convert2ISO.apply(i1.dateProperty().getValue());
		String iso2 = convert2ISO.apply(i2.dateProperty().getValue());
		return iso1.compareTo(iso2);
	}

	private void saveRegExList2File(String fname, List<String> list) {
		try (Writer wr = new FileWriter(fname)) {
			//list.stream().forEach(s -> wr.write(s+"\n")); //exception issue
			for(String s : list) {
				wr.write(s+"\n");
			}
		} catch (IOException e) {
			System.out.println("Не удалось правильно записать файл "+ fname);
		}
	}

	private ObservableList<String> loadRegExListFromFile(String fname) {
		ObservableList<String> list = FXCollections.observableArrayList(Arrays.asList(".*"));
		try (BufferedReader rd = new BufferedReader(new FileReader(fname))) {
			rd.lines().filter(s -> !s.equals(".*")).forEach(s -> list.add(s));
		} catch (IOException e) {
			System.out.println("Не удалось правильно прочитать файл "+ fname);
		}
		return list;
	}

}
