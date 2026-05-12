package playerhub.player.domain;

import java.util.List;

public class ApiResponse {
	private List<PlayerWrapper> response;

	public List<PlayerWrapper> getResponse() {
		return response;
	}

	public void setResponse(List<PlayerWrapper> response) {
		this.response = response;
	}
}
