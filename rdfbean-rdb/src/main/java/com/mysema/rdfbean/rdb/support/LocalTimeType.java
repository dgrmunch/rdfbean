package com.mysema.rdfbean.rdb.support;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Time;
import java.sql.Types;

import org.joda.time.LocalTime;

import com.mysema.query.sql.types.Type;

/**
 * @author tiwe
 *
 */
public class LocalTimeType implements Type<LocalTime>{

    @Override
    public Class<LocalTime> getReturnedClass() {
        return LocalTime.class;
    }

    @Override
    public int[] getSQLTypes() {
        return new int[]{Types.TIME};
    }

    @Override
    public LocalTime getValue(ResultSet rs, int startIndex) throws SQLException {
        Time time = rs.getTime(startIndex);
        return time != null ? new LocalTime(time) : null;
    }

    @Override
    public void setValue(PreparedStatement st, int startIndex, LocalTime value)
        throws SQLException {
        st.setTime(startIndex, new Time(value.getMillisOfDay()));        
    }

}
