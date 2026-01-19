import { useState } from "react";

export default function Admin({ isLoggedIn, setIsLoggedIn }) {

    const[password, setPassword] = useState("");
    const [message, setMessage] = useState("");
    
    const handleLogin = (e) => {
        e.preventDefault();
        if (password === "test") {
            setMessage("Login successful");
            setIsLoggedIn(true);
        } else {
            setMessage("Wrong password");
        }
    };

    const handleLogout = () => {
        setIsLoggedIn(false);
        setPassword("");
    }

    const styles = {
        container: {
        maxWidth: "400px",
        margin: "100px auto",
        padding: "30px",
        boxShadow: "0 0 10px #ccc",
        borderRadius: "8px",
        textAlign: "center"
        },
        form: {
        display: "flex",
        flexDirection: "column",
        gap: "15px"
        },
        input: {
        padding: "10px",
        fontSize: "16px"
        },
        button: {
        padding: "10px",
        fontSize: "16px",
        background: isLoggedIn ? "red" : "#4CAF50",
        color: "#fff",
        border: "none",
        cursor: "pointer"
        }
    };

    return (
        <div style={styles.container}>
            <h2>Login</h2>
                <form style={styles.form}>
                    <input
                    type="password"
                    placeholder="Password"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    style={styles.input}
                    required
                    />
                    <button type="button" onClick={isLoggedIn ? handleLogout : handleLogin}style={styles.button}>{isLoggedIn ? "Logout" : "Login"}</button>
                </form>
            {message && <p>{message}</p>}
        </div>
        );

    
};
