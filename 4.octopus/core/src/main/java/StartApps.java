import com.octopus.utils.exception.ISPException;
import com.octopus.utils.xml.XMLObject;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

/**
 * User: wfgao_000
 * Date: 16-2-16
 * Time: 上午10:46
 */
public class StartApps {
    transient static Log log = LogFactory.getLog(StartApps.class);
    public static void main(String[] args){
        try{
            long l = System.currentTimeMillis();
            if(null !=args)
                XMLObject.loadApplication("classpath:"+args[0], null,true,true);
            else
                XMLObject.loadApplication("classpath:" + System.getProperty("app_path"), null,true,true);
            System.out.println("finish cost time:"+(System.currentTimeMillis()-l)+" ms");
        }catch (Exception e){
            if(e instanceof ISPException){

            }else{
                log.error("start app exception",e);
            }
        }
    }
}
