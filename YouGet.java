import java.net.URL;
import java.io.FileNotFoundException;
import java.io.IOException;

import com.google.gson.JsonObject;

/**
 * Each instance of this class represents a You-Get process.
 * 
 * @author Zhen Chen
 *
 */

public class YouGet implements Runnable {
	private static final int MAX_ATTEMPTS = 3;
	private static String executable;
	private static String charset; // platform dependent
	private URL target;
	private String outputPath = "/";
	private String title;
	private JsonObject info;
	private Task task;
	private boolean forceWrite = false;
	private boolean success;

	public static enum Task {
		INFO, DOWNLOAD;
	}

	// getter and setter for executable
	public static final String getExecutable() {
		return executable;
	}

	/**
	 * If the given path is a directory, locate the first executable program in
	 * it. If the given path is a valid executable program, set it to the
	 * executable.
	 * 
	 * @param path
	 *            a path to a directory containing executable programs or a path
	 *            of an executable program
	 * @throws FileNotFoundException
	 *             if the given path does not contain any executable programs
	 *             and the given path is not a valid executable program either
	 */
	public static final void setExecutable(String path) throws FileNotFoundException {
		executable = Helper.getFirstExecutablePath(path);
	}

	// getter and setter for charset
	public static final String getCharset() {
		return charset;
	}

	public static void setCharset(String charset) {
		YouGet.charset = charset;
	}

	// constructor
	public YouGet(String target) throws IOException {
		this.target = new URL(target);
	}

	public YouGet(URL target) {
		this.target = target;
	}

	public YouGet(String target, String outputPath) throws IOException {
		this(target);
		setOutputPath(outputPath);
	}

	public YouGet(URL target, String outputPath) {
		this(target);
		setOutputPath(outputPath);
	}

	public YouGet(String target, String outputPath, boolean forceWrite) throws IOException {
		this(target, outputPath);
		setForceWrite(forceWrite);
	}

	public YouGet(URL target, String outputPath, boolean forceWrite) {
		this(target, outputPath);
		setForceWrite(forceWrite);
	}

	// getter for target
	public final URL getTarget() {
		return target;
	}

	// getter and setter for outputPath
	public final String getOutputPath() {
		return outputPath;
	}

	public final void setOutputPath(String outputPath) {
		this.outputPath = outputPath;
	}

	// getter for title
	public final String getTitle() {
		return title;
	}

	// getter and setter for task
	public final Task getTask() {
		return task;
	}

	public final void setTask(Task task) {
		this.task = task;
	}

	// getter and setter for forceWrite
	public final boolean getForceWrite() {
		return forceWrite;
	}

	public final void setForceWrite(boolean forceWrite) {
		this.forceWrite = forceWrite;
	}

	// getter for success
	public final boolean isSuccess() {
		return success;
	}

	// two YouGet processes are considered equal if they have the same URL
	@Override
	public boolean equals(Object o) {
		if (o instanceof YouGet) {
			return target.toString().equals(((YouGet) o).getTarget().toString());
		}
		return false;
	}

	@Override
	public int hashCode() {
		return target.toString().hashCode();
	}

	/**
	 * Only MAX_ATTEMPTS number of running times are allowed. If there is a
	 * major exception happened, stop with no more attempts.
	 */
	@Override
	public void run() {
		success = false;
		for (int failedAttempts = 0; failedAttempts < MAX_ATTEMPTS; failedAttempts++) {
			try {
				switch (task) {
				case INFO:
					info();
					break;
				case DOWNLOAD:
					download();
					break;
				}
				success = true;
				task = null;
				break;
			} catch (NoExecutableSetException | IOException e) {
				e.printStackTrace();
				break;
			} catch (ProcessErrorException | InterruptedException e) {
				if (failedAttempts == MAX_ATTEMPTS - 1) {
					// only print error message when failed MAX_ATTEMPTS times
					e.printStackTrace();
				}
			}
		}
	}

	/**
	 * This method is automatically triggered when the object is constructed.
	 * 
	 * It will run the YouGet program to get the info of the target URL and
	 * store the parsed Json result into the info field. At the end, it also
	 * sets up the filename using the title field in the returned info.
	 * 
	 * It needs a user specified charset to read the output of the process
	 * correctly.
	 * 
	 * @throws NoExecutableSetException
	 *             if the executable file location is not set
	 * @throws ProcessErrorException
	 *             if YouGet failed to get info of the target
	 * @throws IOException
	 *             if failed to access or run the program, or if the specified
	 *             charset is invalid
	 * @throws InterruptedException
	 */
	private void info() throws NoExecutableSetException, ProcessErrorException, IOException, InterruptedException {
		if (info != null) {
			return;
		}
		if (executable == null) {
			throw new NoExecutableSetException();
		}
		Process p = new ProcessBuilder(executable, "--json", "\"" + target.toString() + "\"").start();
		ProcessReader pr;
		if (charset == null) {
			pr = new ProcessReader(p);
		} else {
			pr = new ProcessReader(p, charset);
		}
		p.waitFor();
		if (p.exitValue() != 0) {
			throw new ProcessErrorException(pr.getError());
		} else {
			info = Helper.jsonParser.parse(pr.getOutput()).getAsJsonObject();
			try {
				setTarget();
				setTitle();
			} catch (NoInfoOfTargetException e) {
				e.printStackTrace();
			}
		}
	}

	private void download() throws NoExecutableSetException, ProcessErrorException, IOException, InterruptedException {
		info();
		// TODO
	}

}
