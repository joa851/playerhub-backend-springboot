package playerhub.comments.domain;

import java.time.Instant;

import org.hibernate.annotations.CreationTimestamp;

import jakarta.persistence.AttributeOverride;
import jakarta.persistence.AttributeOverrides;
import jakarta.persistence.Column;
import jakarta.persistence.Embedded;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;

@Entity
public class Comment {
	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	private Long playerId;

	private String author;

	@Column(length = 1000)
	private String text;

	private Integer rating;

	@Embedded
	@AttributeOverrides({
	    @AttributeOverride(name = "latitude", column = @Column(name = "location_latitude")),
	    @AttributeOverride(name = "longitude", column = @Column(name = "location_longitude"))
	})
	private Location location;

	@CreationTimestamp
	@Column(name = "created_at", updatable = false)
	private Instant createdAt;

	public Long getId() {
		return id;
	}

	public Long getPlayerId() {
		return playerId;
	}

	public String getAuthor() {
		return author;
	}

	public String getText() {
		return text;
	}

	public Integer getRating() {
		return rating;
	}

	public Location getLocation() {
		return location;
	}

	public Instant getCreatedAt() {
		return createdAt;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public void setPlayerId(Long playerId) {
		this.playerId = playerId;
	}

	public void setAuthor(String author) {
		this.author = author;
	}

	public void setText(String text) {
		this.text = text;
	}

	public void setRating(Integer rating) {
		this.rating = rating;
	}

	public void setLocation(Location location) {
		this.location = location;
	}

	public void setCreatedAt(Instant createdAt) {
		this.createdAt = createdAt;
	}
}
