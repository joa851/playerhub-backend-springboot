package playerhub.player.domain;

import jakarta.persistence.Embeddable;

@Embeddable
public class Birth {

	private String date;
	private String place;
	private String country;

	public String getDate() {
		return date;
	}

	public String getPlace() {
		return place;
	}

	public String getCountry() {
		return country;
	}

	public void setDate(String date) {
		this.date = date;
	}

	public void setPlace(String place) {
		this.place = place;
	}

	public void setCountry(String country) {
		this.country = country;
	}
}
