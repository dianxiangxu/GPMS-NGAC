package gpms.rest;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URISyntaxException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.log4j.Logger;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import com.fasterxml.jackson.databind.ObjectMapper;

@Path("/files")
// @Api(value = "/files", description = "Manage Files")
@Consumes({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML,
		MediaType.APPLICATION_FORM_URLENCODED })
@Produces({ MediaType.APPLICATION_JSON, MediaType.APPLICATION_XML,
		MediaType.TEXT_PLAIN })
public class FileService {

	private static final Logger log = Logger
			.getLogger(NotificationService.class.getName());

	@GET
	@Path("/download")
	@Consumes({ MediaType.MULTIPART_FORM_DATA })
	@Produces(MediaType.APPLICATION_OCTET_STREAM)
	@ApiOperation(value = "Download File", notes = "This API downloads file from the server")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Success: { true }"),
			@ApiResponse(code = 404, message = "Failed: { \"error\":\"error description\", \"status\": \"FAIL\" }") })
	public Response downloadFile(@Context HttpServletRequest request,
			@Context HttpServletResponse response) throws ServletException {
		try {
			log.info("FileService::downloadFile started");

			String fileName = request.getParameter("fileName");
			if (fileName == null || fileName.equals("")) {
				throw new ServletException("File Name can't be null or empty");
			}

			String DOWNLOAD_PATH = new String();
			try {
				DOWNLOAD_PATH = this.getClass().getResource("/uploads").toURI()
						.getPath();
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}

			// File file = new File(request.getServletContext().getAttribute(
			// "FILES_DIR")
			// + File.separator + fileName);

			File file = new File(DOWNLOAD_PATH + fileName);
			if (!file.exists()) {
				// throw new ServletException("File doesn't exists on server.");
				return Response.status(Response.Status.NOT_FOUND)
						.entity("FILE NOT FOUND: " + fileName)
						.type("text/plain").build();
			}

			System.out.println("File location on server::"
					+ file.getAbsolutePath());
			ServletContext ctx = request.getServletContext();
			InputStream fis = new FileInputStream(file);
			String mimeType = ctx.getMimeType(file.getAbsolutePath());
			response.setContentType(mimeType != null ? mimeType
					: "application/octet-stream");
			response.setContentLength((int) file.length());
			response.setHeader("Content-Disposition", "attachment; filename=\""
					+ fileName + "\"");

			ServletOutputStream os = response.getOutputStream();
			byte[] bufferData = new byte[1024];
			int read = 0;
			while ((read = fis.read(bufferData)) != -1) {
				os.write(bufferData, 0, read);
			}
			os.flush();
			os.close();
			fis.close();
			System.out.println("File downloaded at client successfully");

			return Response.ok(true).build();
		} catch (Exception e) {
			log.error("Could not find the requested file error e=", e);
		}

		return Response
				.status(Response.Status.NOT_FOUND)
				.entity("{\"error\": \"Could Not Find The Requested File\", \"status\": \"FAIL\"}")
				.build();
	}

	@POST
	@Path("/upload")
	@Consumes({ MediaType.MULTIPART_FORM_DATA })
	@ApiOperation(value = "Upload File", notes = "This API uploads file to the server")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Success: { true }"),
			@ApiResponse(code = 404, message = "Failed: { \"error\":\"error description\", \"status\": \"FAIL\" }") })
	public Response uploadFile(
			@ApiParam(value = "file", required = true, defaultValue = "", allowableValues = "", allowMultiple = false) @FormDataParam("file") InputStream fileInputStream,
			@ApiParam(value = "file", required = true, defaultValue = "", allowableValues = "", allowMultiple = false) @FormDataParam("file") FormDataContentDisposition fileMetaData)
			throws ServletException {
		try {
			log.info("FileService::uploadFile started");

			String UPLOAD_PATH = new String();
			try {
				UPLOAD_PATH = this.getClass().getResource("/uploads").toURI()
						.getPath();
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}

			try {
				int read = 0;
				byte[] bytes = new byte[1024];

				OutputStream out = new FileOutputStream(new File(UPLOAD_PATH
						+ fileMetaData.getFileName()));
				while ((read = fileInputStream.read(bytes)) != -1) {
					out.write(bytes, 0, read);
				}
				out.flush();
				out.close();
			} catch (IOException e) {
				// throw new WebApplicationException(
				// "Error while uploading file. Please try again !!");
				return Response
						.status(403)
						.entity("Error while uploading file. Please try again !!")
						.type("text/plain").build();
				// Response.status(403).type("text/plain").entity(message).build();
			}
			return Response.ok(true).build();
		} catch (Exception e) {
			log.error("Could not upload the file error e=", e);
		}

		return Response
				.status(Response.Status.BAD_REQUEST)
				.entity("{\"error\": \"Could Not Upload The File\", \"status\": \"FAIL\"}")
				.build();
	}

	@POST
	@Path("/multiupload")
	@Consumes({ MediaType.MULTIPART_FORM_DATA })
	@Produces({ MediaType.TEXT_PLAIN })
	@ApiOperation(value = "Upload Multiple Files", notes = "This API uploads multiple files to the server")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Success: { File Name }"),
			@ApiResponse(code = 404, message = "Failed: { \"error\":\"error description\", \"status\": \"FAIL\" }") })
	public Response multiUploadFile(@Context HttpServletRequest request,
			@Context HttpServletResponse response) throws ServletException {
		try {
			log.info("FileService::multiUploadFile started");

			if (request == null) {
				throw new ServletException("Request can't be null or empty");
			}

			if (!ServletFileUpload.isMultipartContent(request)) {
				throw new ServletException(
						"Content type is not multipart/form-data");
			}

			String UPLOAD_PATH = new String();
			try {
				UPLOAD_PATH = this.getClass().getResource("/uploads").toURI()
						.getPath();
			} catch (URISyntaxException e) {
				e.printStackTrace();
			}

			DiskFileItemFactory factory = new DiskFileItemFactory();
			ServletFileUpload upload = new ServletFileUpload(factory);

			List<FileItem> fileItemsList = upload.parseRequest(request);
			if (fileItemsList == null || fileItemsList.isEmpty()) {
				throw new ServletException("File Name can't be null or empty");
			}

			Iterator<FileItem> fileItemsIterator = fileItemsList.iterator();
			while (fileItemsIterator.hasNext()) {
				FileItem fileItem = fileItemsIterator.next();
				String fileExtension = getFileExtension(fileItem.getName());

				System.out.println("FieldName=" + fileItem.getFieldName());
				System.out.println("FileName=" + fileItem.getName());
				System.out.println("FileExtension=" + fileExtension);
				System.out.println("ContentType=" + fileItem.getContentType());
				System.out.println("Size in bytes=" + fileItem.getSize());

				SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd");
				Date date = new Date();
				System.out.println(); // 2016/02/10 16:16:39

				String fileName = String.format("%s.%s",
						RandomStringUtils.randomAlphanumeric(8) + "_"
								+ dateFormat.format(date), fileExtension);

				File file = new File(UPLOAD_PATH + fileName);

				// File file = new
				// File(request.getServletContext().getAttribute(
				// "FILES_DIR")
				// + File.separator + fileItem.getName());
				System.out.println("Absolute Path at server="
						+ file.getAbsolutePath());
				fileItem.write(file);

				// 2. Set response type to html
				response.setContentType("text/html");

				// 3. Convert List<FileMeta> into JSON format
				ObjectMapper mapper = new ObjectMapper();

				// 4. Send result to client
				mapper.writeValue(response.getOutputStream(), fileName);
			}

		} catch (Exception e) {
			log.error("Could not upload multiple files error e=", e);
		}

		return Response
				.status(Response.Status.BAD_REQUEST)
				.entity("{\"error\": \"Could Not Upload Multiple Files\", \"status\": \"FAIL\"}")
				.build();
	}

	private static String getFileExtension(String fileName) {
		if (fileName.lastIndexOf(".") != -1 && fileName.lastIndexOf(".") != 0)
			return fileName.substring(fileName.lastIndexOf(".") + 1);
		else
			return "";
	}

	@POST
	@Path("/delete")
	// @Consumes({ MediaType.MULTIPART_FORM_DATA })
	@Produces({ MediaType.TEXT_PLAIN })
	@ApiOperation(value = "Delete the selected file", notes = "This API deletes the selected file")
	@ApiResponses(value = {
			@ApiResponse(code = 200, message = "Success: { true }"),
			@ApiResponse(code = 400, message = "Failed: { \"error\":\"error description\", \"status\": \"FAIL\" }") })
	public Response deleteFile(@Context HttpServletRequest request,
			@Context HttpServletResponse response) throws ServletException {
		try {
			log.info("FileService::deleteFile started");

			if (request == null) {
				throw new ServletException("Request can't be null or empty");
			}

			// 2. Set response type to html
			response.setContentType("text/html");

			// 3. Convert List<FileMeta> into JSON format
			ObjectMapper mapper = new ObjectMapper();

			// 4. Send result to client
			mapper.writeValue(response.getOutputStream(), true);
		} catch (Exception e) {
			log.error("Could not delete the selected file error e=", e);
		}

		return Response
				.status(Response.Status.BAD_REQUEST)
				.entity("{\"error\": \"Could Not Delete The Selected File\", \"status\": \"FAIL\"}")
				.build();
	}

}
