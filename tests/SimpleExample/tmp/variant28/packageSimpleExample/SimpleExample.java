package packageSimpleExample;

public class SimpleExample {

    public int mid(int x, int y, int z){

        int ret;
	ret = y;
	if(y<z){
	   if(x<y){
		ret = y;
	   }else if(x<z){
		{
		}
	   }
	}else{
	   if(x>y){
		ret = y;
	   }else if(x>z){
		ret = x;
	   }	
	}
	return ret;
    }
}