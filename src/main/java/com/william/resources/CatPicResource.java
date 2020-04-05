package com.william.resources;


import com.william.CatPicServiceConfiguration;
import com.william.core.CatPic;
import com.william.db.CatPicDao;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;

import javax.imageio.ImageIO;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.awt.image.BufferedImage;
import java.io.*;
import java.util.List;

import static com.william.core.Constants.UPLOAD_FILE_ROOT;

@Path("/cats")
@Produces(MediaType.APPLICATION_JSON)
public class CatPicResource {
    private static final Log logger = LogFactory.getFactory().getInstance(CatPicResource.class);
    private CatPicDao catPicDao;
    private CatPicServiceConfiguration configuration;

    public CatPicResource(CatPicDao catPicDao, CatPicServiceConfiguration configuration) {
        this.catPicDao = catPicDao;
        this.configuration = configuration;
    }

    /**
     * @param id The id of the pic to be retrieved.
     * @return The cat pic for the given id as a png file, or BAD_REQUEST if a null id was passed in,
     * NO_CONTENT if the file could not be read,
     * or NOT_FOUND if an unknown id was provided,
     * or INTERNAL_SERVER_ERROR if unable to read the image file from the filesystem.
     * @throws Exception
     */
    @GET
    @Produces("image/png")
    @Path("/{id}")
    public Response getFile(@PathParam("id") Integer id) throws Exception {
        if (id == null) {
            logger.info("Attempt to get a null fileId");
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        CatPic catPic = catPicDao.findByid(id);

        if (catPic == null) {
            logger.info("Attempt to get an unknown fileId=" + id);
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        File file = new File(catPic.getFilePath());
        BufferedImage image;
        try {
            image = ImageIO.read(file);
        } catch (IOException e) {
            logger.info("Unable to read image file for id=" + id);
            return Response.status(Response.Status.NO_CONTENT).build();
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try{
        ImageIO.write(image, "png", baos);
            byte[] imageData = baos.toByteArray();
            return Response.ok(imageData).build();
        }catch(Exception e){
            logger.error("Unknown error while reading image from filesystem for id=" + id);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    /**
     * @param id The id of the cat pic that is to be deleted.
     * @return The same id that was provided if deletion was successful,
     * BAD_REQUEST if id=null,
     * NOT_FOUND if id is unknown,
     * INTERNAL_SERVER_ERROR if we were unable to delete the record from the database for a known id
     */
    @DELETE
    @Produces({MediaType.APPLICATION_JSON})
    @Path("/{id}")
    public Response deleteFile(@PathParam("id") Integer id) {
        if (id == null) {
            logger.info("Attempt to delete a null fileId");
            return Response.status(Response.Status.BAD_REQUEST).build();
        }
        CatPic catPic = catPicDao.findByid(id);
        if (catPic == null) {
            logger.info("Attempt to delete an unknown fileId=" + id);
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        int rowsDeleted;
        try {
            rowsDeleted = catPicDao.deleteByid(id);
        } catch (Exception e) {
            logger.error("An unexpected error occurred while trying to delete database entry for id=" + id);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }

        if (rowsDeleted == 1) {
            try {
                deleteFile(catPic);
            } catch (Exception e) {
                logger.warn("Unable to remove file from filesystem: " + catPic.getFilePath());
            }
            return Response.ok(id).build();
        } else {
            logger.warn("Data inconsistency; id=" + id + " was unexpectedly deleted outside of this rquest.");
            return Response.ok(id).build();
        }
    }

    /**
     * This endpoint returns a list of all known ids.
     * <p>
     * Possible future enhancement, we may wish to return a list of id's AND images, but we would want to require
     * the caller to provide some criteria for how tha list is formed; it should not be unbounded. Some examples of
     * criteria: a) most recent 100, b) least recent 100, or c) a specific set of ids. In the case of c), we would
     * force the caller to figure out how to paginate through the set of ids.
     *
     * @return A list of ids.
     * @throws Exception
     */
    @GET
    @Path("ids")
    @Produces({MediaType.APPLICATION_JSON})
    public Response getIds() throws Exception {
        List<Integer> ids = catPicDao.getIds();
        return Response.ok(ids).build();
    }

    /**
     * @param idAsString          An optional parameter. If specified, then this method will update the image previously
     *                            associated with this ID.
     * @param uploadedInputStream The image file being uploaded.
     * @param fileDetail          The name of the image file being uploaded.
     * @return The server-generated (or client provided) id for the given file.
     * BAD_REQUEST: If an attempt was made to update the image for an un-parsable id,
     * NOT_FOUND: If an attempt was made to update the image for an unknown id,
     * INTERNAL_SERVER_ERROR: If the id was found, but we were unable to perform the database update on it.
     * @throws IOException
     */
    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces({MediaType.APPLICATION_JSON})
    public Response uploadFile(
            @DefaultValue("") @FormDataParam("id") String idAsString,
            @FormDataParam("file") InputStream uploadedInputStream,
            @FormDataParam("file") FormDataContentDisposition fileDetail) throws IOException {

        if ("".equals(idAsString.trim())) {
            //then this is a completely new file; immediately upload it
            return uploadNewFile(uploadedInputStream, fileDetail);
        }

        int idAsInt;
        try {
            idAsInt = Integer.parseInt(idAsString);
        } catch (NumberFormatException e) {
            logger.info("Attempt to update a cat pic using an un-parsable id: " + idAsString);
            return Response.status(Response.Status.BAD_REQUEST).build();
        }

        CatPic existingCatPic = catPicDao.findByid(idAsInt);
        if (existingCatPic == null) {
            logger.info("Attempt to update pic for an unknown id=" + idAsInt);
            return Response.status(Response.Status.NOT_FOUND).build();
        }

        File uploadedFileObj = new File(UPLOAD_FILE_ROOT + fileDetail.getFileName());
        int numRowsUpdated = updateFile(idAsInt, uploadedFileObj, uploadedInputStream, fileDetail);

        if (numRowsUpdated == 1) {
            deleteFile(existingCatPic);
            return Response.ok(idAsInt).build();
        } else {
            logger.error("Unexpected error; database record was found, but we were unable to update it. id=" + idAsInt);
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).build();
        }
    }

    private void deleteFile(CatPic existingCatPic) {
        try {
            File fileObj = new File(existingCatPic.getFilePath());
            boolean fileWasDeleted = fileObj.delete();
            if (!fileWasDeleted) {
                logger.warn("Unable to remove file from filesystem: " + existingCatPic.getFilePath());
            }
        } catch (Exception e) {
            logger.warn("id=" + existingCatPic.getId() + " is now orphaned due to unexpected error while trying to remove file from filesystem: " + existingCatPic.getFilePath(), e);
        }
    }

    /**
     * @param idAsInt The id of the cat pic we want to update.
     * @param uploadedInputStream The cat pic.
     * @param fileDetail The filename of the cat pic.
     * @return The number of rows updated.
     * @throws IOException If we were unable to write the file to the filesystem.
     */
    private int updateFile(int idAsInt, File uploadedFileObj, InputStream uploadedInputStream, FormDataContentDisposition fileDetail) throws IOException {
        logger.debug("Attempting to update existing id=" + idAsInt + " with new file=" + uploadedFileObj.getAbsolutePath());
        writeToFile(uploadedInputStream, uploadedFileObj);
        return catPicDao.update(idAsInt, uploadedFileObj.getAbsolutePath());
    }

    private Response uploadNewFile(InputStream uploadedInputStream, FormDataContentDisposition fileDetail) throws IOException {
        File uploadedFileObj = new File(UPLOAD_FILE_ROOT + fileDetail.getFileName());

        logger.debug("Attempting to persist file: " + uploadedFileObj.getAbsolutePath());
        writeToFile(uploadedInputStream, uploadedFileObj);

        int id = catPicDao.insert(uploadedFileObj.getAbsolutePath());
        return Response.ok(id).build();
    }

    private void writeToFile(InputStream uploadedInputStream, File uploadedFileLocation) throws IOException {
        int read;
        final int BUFFER_LENGTH = 1024;
        final byte[] buffer = new byte[BUFFER_LENGTH];
        OutputStream out = new FileOutputStream(uploadedFileLocation);
        int totalBytesRead = 0;
        while ((read = uploadedInputStream.read(buffer)) != -1) {
            totalBytesRead += read;
            if(totalBytesRead > (configuration.getMaxCatPicFileSizeMb() * 1000000)){
                throw new IOException("Maximum file size exceeded. Configured max. file size (MB) is: " + configuration.getMaxCatPicFileSizeMb());
            }
            out.write(buffer, 0, read);
        }
        out.flush();
        out.close();
    }
}
