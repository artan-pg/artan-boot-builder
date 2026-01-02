package ir.artanpg.boot.kernel.Sample;

import java.util.Objects;
import java.util.StringJoiner;

public class UserEntity {

	private String id;
	private String username;
	private String password;

	public UserEntity() {
	}

	public UserEntity(String id) {
		this.id = id;
	}

	public UserEntity(String id, String username, String password) {
		this.id = id;
		this.username = username;
		this.password = password;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getUsername() {
		return username;
	}

	public void setUsername(String username) {
		this.username = username;
	}

	public String getPassword() {
		return password;
	}

	public void setPassword(String password) {
		this.password = password;
	}

	@Override
	public boolean equals(Object o) {
		if (!(o instanceof UserEntity that)) return false;
		return Objects.equals(id, that.id) && Objects.equals(username, that.username) && Objects.equals(password, that.password);
	}

	@Override
	public int hashCode() {
		return Objects.hash(id, username, password);
	}

	@Override
	public String toString() {
		return new StringJoiner(", ", UserEntity.class.getSimpleName() + "[", "]")
				.add("id='" + id + "'")
				.add("username='" + username + "'")
				.add("password='" + password + "'")
				.toString();
	}
}
