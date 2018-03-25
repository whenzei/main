package seedu.address.model.session;

import static java.util.Objects.requireNonNull;
import static seedu.address.model.job.Status.STATUS_CLOSED;
import static seedu.address.model.job.Status.STATUS_ONGOING;
import static seedu.address.model.job.VehicleNumber.isValidVehicleNumber;
import static seedu.address.model.person.Email.isValidEmail;
import static seedu.address.model.person.Name.isValidName;
import static seedu.address.model.person.Phone.isValidPhone;
import static seedu.address.model.remark.Remark.isValidRemark;
import static seedu.address.model.session.SheetParser.APPROVAL_STATUS;
import static seedu.address.model.session.SheetParser.CLIENT_EMAIL;
import static seedu.address.model.session.SheetParser.CLIENT_NAME;
import static seedu.address.model.session.SheetParser.CLIENT_PHONE;
import static seedu.address.model.session.SheetParser.EMPLOYEE_EMAIL;
import static seedu.address.model.session.SheetParser.EMPLOYEE_NAME;
import static seedu.address.model.session.SheetParser.EMPLOYEE_PHONE;
import static seedu.address.model.session.SheetParser.REMARKS;
import static seedu.address.model.session.SheetParser.STATUS;
import static seedu.address.model.session.SheetParser.VEHICLE_NUMBER;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;

import java.util.Iterator;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.Sheet;

import org.apache.poi.ss.usermodel.Workbook;
import seedu.address.model.job.Date;
import seedu.address.model.job.JobNumber;
import seedu.address.model.job.Status;
import seedu.address.model.job.VehicleNumber;
import seedu.address.model.person.Email;
import seedu.address.model.person.Employee;
import seedu.address.model.person.Name;
import seedu.address.model.person.Person;
import seedu.address.model.person.Phone;
import seedu.address.model.person.UniqueEmployeeList;
import seedu.address.model.person.exceptions.DuplicateEmployeeException;
import seedu.address.model.remark.Remark;
import seedu.address.model.remark.RemarkList;
import seedu.address.model.session.exceptions.DataIndexOutOfBoundsException;
import seedu.address.model.session.exceptions.FileFormatException;

//@@uathor yuhongherald
/**
 * A data structure used to store header field information of a given excel sheet
 */
public class SheetWithHeaderFields implements Iterable<JobEntry> {
    private static final String ERROR_MESSAGE_EMPTY_SHEET = "Sheet %d contains no valid job entries!";

    private final Sheet sheet;
    private final HashMap<String, RowData> compulsoryFields;
    private final HashMap<String, RowData> commentFields;
    private final HashMap<String, RowData> optionalFields;
    private final JobEntry firstJobEntry;

    SheetWithHeaderFields(Sheet sheet, HashMap<String, RowData> commentFields, HashMap<String, RowData> compulsoryFields,
                          HashMap<String, RowData> optionalFields) throws FileFormatException {
        this.sheet = sheet;
        this.compulsoryFields = new HashMap<>(compulsoryFields);
        this.commentFields = new HashMap<>(commentFields);
        this.optionalFields = new HashMap<>(optionalFields);
        firstJobEntry = getFirstJobEntry();
        if (firstJobEntry == null) {
            throw new FileFormatException(String.format(ERROR_MESSAGE_EMPTY_SHEET,
                    sheet.getWorkbook().getSheetIndex(sheet)));
        }
    }

    /**
     * 
     * @param row
     */
    public void rejectJobEntry(int row) {
        int index = commentFields.get(APPROVAL_STATUS).getStartIndex();
        Cell cell = sheet.getRow(row).getCell(index);
        cell.setCellValue("rejected");
    }

    public void approveJobEntry(int row) {
        ;
    }

    public void commentJobEntry(int row) {
        ;
    }

    /**
     * Looks for first (@code JobEtnry) with no missing fields and returns it
     * @throws FileFormatException if no valid job entries
     */
    private JobEntry getFirstJobEntry() throws FileFormatException {
        Person client;
        VehicleNumber vehicleNumber;
        Employee employee;

        UniqueEmployeeList employeeList;
        Status status;
        RemarkList remarkList;
        for (int i = sheet.getFirstRowNum(); i <= sheet.getLastRowNum(); i++) {
            client = getClient(i);
            vehicleNumber = getVehicleNumber(i);
            employee = getEmployee(i);
            if (client == null || vehicleNumber == null || employee == null) {
                continue;            // TODO: add comment that it is corrupted under feedback
            }
            employeeList = new UniqueEmployeeList();
            try {
                employeeList.add(employee);
            } catch (DuplicateEmployeeException e) {
                e.printStackTrace(); // should not happen
            }
            status = getStatus(i);
            remarkList = getRemarks(i);
            return new JobEntry(client, vehicleNumber, new JobNumber(), new Date(), employeeList, status, remarkList,
                sheet.getWorkbook().getSheetIndex(sheet), i);
        }
        throw new FileFormatException(String.format(ERROR_MESSAGE_EMPTY_SHEET,
                sheet.getWorkbook().getSheetIndex(sheet)));
    }

    private Person getClient(int rowNumber) {
        String name = readFirstData(compulsoryFields.get(CLIENT_NAME), rowNumber);
        String phone = readFirstData(compulsoryFields.get(CLIENT_PHONE), rowNumber);
        String email = readFirstData(compulsoryFields.get(CLIENT_EMAIL), rowNumber);
        if (isValidName(name) && isValidPhone(phone) && isValidEmail(email)) {
            return new Person(new Name(name), new Phone(phone), new Email(email));
        }
        return null;
    }

    private VehicleNumber getVehicleNumber(int rowNumber) {
        String vehicleNumber = readFirstData(compulsoryFields.get(VEHICLE_NUMBER), rowNumber);
        if (isValidVehicleNumber(vehicleNumber)) {
            return new VehicleNumber(vehicleNumber);
        }
        return null;
    }

    private Employee getEmployee(int rowNumber) {
        String name = readFirstData(compulsoryFields.get(EMPLOYEE_NAME), rowNumber);
        String phone = readFirstData(compulsoryFields.get(EMPLOYEE_PHONE), rowNumber);
        String email = readFirstData(compulsoryFields.get(EMPLOYEE_EMAIL), rowNumber);
        if (isValidName(name) && isValidPhone(phone) && isValidEmail(email)) {
            return new Employee(new Name(name), new Phone(phone), new Email(email), Collections.emptySet());
        }
        return null;
    }

    private Status getStatus(int rowNumber) {
        RowData optionalStatus = optionalFields.get(STATUS);
        if (optionalStatus == null) {
            return new Status(STATUS_ONGOING);
        }
        String status = readFirstData(optionalStatus, rowNumber).toLowerCase();
        if (status.equals((STATUS_CLOSED))) {
            return new Status(status);
        }
        return new Status(STATUS_ONGOING);
    }

    private RemarkList getRemarks(int rowNumber) {
        RowData optionalRemarks = optionalFields.get(REMARKS);
        if (optionalRemarks == null) {
            return new RemarkList();
        }
        RemarkList remarkList = new RemarkList();
        ArrayList<String> remarks = readListData(optionalFields.get(REMARKS), rowNumber);
        for (String remark : remarks) {
            if (isValidRemark(remark)) {
                remarkList.add(new Remark(remark));
            }
        }
        return remarkList;
    }

    /**
     * Reads single entry (@ocde rowDta) from row (@code rowNumber)
     */
    private String readFirstData(RowData rowData, int rowNumber) {
        requireNonNull(rowData);
        try {
            return rowData.readDataFromSheet(sheet, rowNumber).get(0);
        } catch (DataIndexOutOfBoundsException e) {
            throw new IndexOutOfBoundsException(e.getMessage()); // should be within bounds
        }
    }

    /**
     * Reads all entries (@ocde rowDta) from row (@code rowNumber)
     */
    private ArrayList<String> readListData(RowData rowData, int rowNumber) {
        requireNonNull(rowData);
        try {
            return rowData.readDataFromSheet(sheet, rowNumber);
        } catch (DataIndexOutOfBoundsException e) {
            throw new IndexOutOfBoundsException(e.getMessage()); // should be within bounds
        }
    }
    /**
     * Retrieves the job at (@code rowNumber), and missing details from those in first job entry.
     * Adds missing detail in remarks.
     */
    private JobEntry getJobEntryAt(int rowNumber, JobEntry previousEntry) {
        // TODO: add comment that it is corrupted under feedback
        Person client = getClient(rowNumber);
        VehicleNumber vehicleNumber = getVehicleNumber(rowNumber);
        Employee employee = getEmployee(rowNumber);
        if (client == null || vehicleNumber == null || employee == null) {
            ; // Need to set default for them
        }
        UniqueEmployeeList employeeList = new UniqueEmployeeList();
        try {
            employeeList.add(employee);
        } catch (DuplicateEmployeeException e) {
            e.printStackTrace(); // should not happen
        }
        Status status = getStatus(rowNumber);
        RemarkList remarkList = getRemarks(rowNumber);
        return new JobEntry(client, vehicleNumber, new JobNumber(), new Date(), employeeList, status, remarkList,
                sheet.getWorkbook().getSheetIndex(sheet), rowNumber);
    }

    @Override
    public Iterator<JobEntry> iterator() {
        return new Iterator<JobEntry>() {
            private int currentRow = firstJobEntry.getRowNumber();
            private JobEntry previousEntry;

            @Override public boolean hasNext() {
                return (currentRow < sheet.getLastRowNum());
            }

            @Override public JobEntry next() {
                if (!hasNext()) {
                    return null;
                }
                previousEntry = getJobEntryAt(currentRow, previousEntry);
                currentRow++;
                return previousEntry;
            }
        };
    }
}
