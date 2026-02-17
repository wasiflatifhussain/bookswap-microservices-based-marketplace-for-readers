export function isValidPassword(password: string): boolean {
  // min 8 chars, 1 number, 1 special char
  const regex = /^(?=.*[0-9])(?=.*[!@#$%^&*]).{8,}$/;
  return regex.test(password);
}
