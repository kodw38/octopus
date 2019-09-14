import com.octopus.utils.alone.StringUtils;

public class TestStart {
    public static void main(String[] args){
        try{
            StartApps.main(new String[]{"GetImageFromWebsite.cell"});
            //StartApps.main(new String[]{"stock-bg.isp"});
            //String t = StringUtils.replaceAllWithReg("http://mm.com/cc/aa/../../bb/a.html","/[^/]+/\\.\\./","/");
            //System.out.println(t);
        }catch (Exception e){
            e.printStackTrace();
        }
    }
}
