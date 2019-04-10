package com.octopus.tools.filelog;

/**
 * User: wfgao_000
 * Date: 15-11-20
 * Time: 下午10:16
 */
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.Locale;
import java.util.TimeZone;

class RollingCalendar extends GregorianCalendar
{
    int type = -1;

    RollingCalendar(TimeZone tz, Locale locale)
    {
        super(tz, locale);
    }

    void setType(int type) {
        this.type = type;
    }

    public long getNextCheckMillis(Date now) {
        return getNextCheckDate(now, 1).getTime();

    }

    public Date getDeltaDate(Date now, int delta) {
        return getNextCheckDate(now, delta);
    }

    public Date getNextCheckDate(Date now, int delta) {
        setTime(now);

        switch (this.type)
        {
            case 0:
                set(13, 0);
                set(14, 0);
                add(12, delta);
                break;
            case 1:
                set(12, 0);
                set(13, 0);
                set(14, 0);
                add(11, delta);
                break;
            case 2:
                set(12, 0);
                set(13, 0);
                set(14, 0);
                add(11, delta * 12);
                break;
            case 3:
                set(11, 0);
                set(12, 0);
                set(13, 0);
                set(14, 0);
                add(5, delta);
                break;
            case 4:
                set(7, getFirstDayOfWeek());
                set(11, 0);
                set(13, 0);
                set(14, 0);
                add(3, delta);
                break;
            case 5:
                set(5, 1);
                set(11, 0);
                set(13, 0);
                set(14, 0);
                add(2, delta);
                break;
            default:
                throw new IllegalStateException("Unknown periodicity type.");
        }
        return getTime();
    }
}
