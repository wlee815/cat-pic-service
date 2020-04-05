package com.william;

import com.william.db.CatPicDao;
import com.william.resources.CatPicResource;
import io.dropwizard.Application;
import io.dropwizard.assets.AssetsBundle;
import io.dropwizard.db.DataSourceFactory;
import io.dropwizard.jdbi.DBIFactory;
import io.dropwizard.setup.Bootstrap;
import io.dropwizard.setup.Environment;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.skife.jdbi.v2.DBI;
import org.skife.jdbi.v2.Handle;

import java.io.File;

import static com.william.core.Constants.UPLOAD_FILE_ROOT;

public class CatPicServiceApplication extends Application<CatPicServiceConfiguration> {

    public static void main(final String[] args) throws Exception {
        new CatPicServiceApplication().run(args);
    }

    @Override
    public String getName() {
        return "CatPicService";
    }

    @Override
    public void initialize(final Bootstrap<CatPicServiceConfiguration> bootstrap) {
        bootstrap.addBundle(new AssetsBundle("/assets/", "/cat-pic-service"));
    }

    @Override
    public void run(final CatPicServiceConfiguration configuration,
                    final Environment environment) {
        environment.jersey().register(MultiPartFeature.class);

        File uploadFileRootObj = new File(UPLOAD_FILE_ROOT);
        uploadFileRootObj.mkdir(); //creates if it doesn't already exist

        final DataSourceFactory dataSourceFactory = new DataSourceFactory();
        dataSourceFactory.setDriverClass("org.h2.Driver");
        dataSourceFactory.setUrl("jdbc:h2:mem:test-" + System.currentTimeMillis() + "?user=sa");
        dataSourceFactory.setInitialSize(1);
        final DBIFactory factory = new DBIFactory();
        final DBI dbi = factory.build(environment, dataSourceFactory, "h2");
        try (Handle h = dbi.open()) {
            h.execute("CREATE TABLE cat_pics(" +
                    "id INT AUTO_INCREMENT, " +
                    "file_path VARCHAR(1024) NOT NULL" +
                    ")");
        }
        CatPicDao catPicDao = dbi.onDemand(CatPicDao.class);

        final CatPicResource catResource = new CatPicResource(catPicDao, configuration);
        environment.jersey().register(catResource);
    }

}
