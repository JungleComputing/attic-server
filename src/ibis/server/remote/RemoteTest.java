package ibis.server.remote;

public class RemoteTest {

	public static void main(String[] args) throws Exception {
		ProcessBuilder builder = new ProcessBuilder();
		builder.command().add("ipl-server");
		builder.command().add("--remote");

		Process process = builder.start();

		System.err.println("started server");

		RemoteClient client = new RemoteClient();

		new OutputForwarder(process.getInputStream(), client.getOutputStream());
		new OutputForwarder(client.getInputStream(), process.getOutputStream());
		new OutputForwarder(process.getErrorStream(), System.err);

		System.err.println("started client");

		System.err
				.println("server local address = " + client.getLocalAddress());

		client.addHubs("localhost:5332");
		String[] hubs = client.getHubs();

		for (String hub : hubs) {
			System.err.println("hub = " + hub);
		}

		System.err.println("ending server");
		client.end(false);
		System.err.println("ended");

		process.destroy();
	}

}
