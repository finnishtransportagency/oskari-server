package org.oskari.service.backendstatus.maplayer;

import java.util.List;

import javax.sql.DataSource;

import org.apache.ibatis.session.SqlSession;
import org.apache.ibatis.session.SqlSessionFactory;

import fi.nls.oskari.db.DatasourceHelper;
import fi.nls.oskari.mybatis.MyBatisHelper;

/**
 * Data access to basic information of MapLayers
 */
public class MapLayerDao {

    private final SqlSessionFactory factory;

    public MapLayerDao() {
        this(DatasourceHelper.getInstance().getDataSource());
    }

    public MapLayerDao(final DataSource ds) {
        this.factory = MyBatisHelper.initMyBatis(ds, MapLayer.class);
    }

    public List<MapLayer> findWMSMapLayers() {
        try (SqlSession session = factory.openSession()) {
            return session.getMapper(MapLayerMapper.class).selectWMSLayers();
        }
    }

    public List<MapLayer> findWFSMapLayers() {
        try (SqlSession session = factory.openSession()) {
            return session.getMapper(MapLayerMapper.class).selectWFSLayers();
        }
    }

}