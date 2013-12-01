package au.com.addstar.pandora;

public interface Module
{
	public void onEnable();
	
	public void onDisable();
	
	public void setPandoraInstance(MasterPlugin plugin);
}
