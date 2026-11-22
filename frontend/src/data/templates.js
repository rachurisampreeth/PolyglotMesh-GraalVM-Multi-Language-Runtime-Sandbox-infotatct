export const initialInput = {
  product: 'Starter Plan',
  unitPrice: 1499,
  quantity: 3,
  coupon: 'SAVE10',
};

export const localTemplates = [
  {
    id: 'pricing-engine', title: 'Dynamic pricing engine', subtitle: 'Java → Python → JavaScript',
    mode: 'pipeline', language: 'python', code: '', input: initialInput,
    pythonCode: `# The Java host provides input_data through GraalVM
subtotal = input_data['unitPrice'] * input_data['quantity']
discount = 0.10 if input_data['coupon'] == 'SAVE10' else 0
result = round(subtotal * (1 - discount), 2)
print('Python pricing result: INR', result)
`,
    javascriptCode: `// Validate the Python result in the SAME Java process
const payable = Number(python_result);
const approved = payable > 0 && payable < 100000;
result = JSON.stringify({
  approved, payable, currency: 'INR',
  source: 'GraalPy → GraalJS'
});
print('JS fraud-check approved:', approved);
`,
  },
  {
    id: 'python-hello', title: 'Python data processing', subtitle: 'GraalPy single language',
    mode: 'single', language: 'python', input: initialInput,
    code: `# Execute Python directly inside the Java VM
values = [10, 20, 30, 40]
result = sum(values) / len(values)
print('Computed mean:', result)
`, pythonCode: '', javascriptCode: '',
  },
  {
    id: 'javascript-hello', title: 'JavaScript validation', subtitle: 'GraalJS single language',
    mode: 'single', language: 'javascript', input: initialInput,
    code: `const price = Number(input_data.unitPrice);
const qty = Number(input_data.quantity);
result = JSON.stringify({subtotal: price * qty, valid: qty > 0});
print('Calculated in JavaScript:', price * qty);
`, pythonCode: '', javascriptCode: '',
  },
  {
    id: 'blocked-access', title: 'Permission check', subtitle: 'Demonstrate restricted host access',
    mode: 'single', language: 'javascript', input: initialInput,
    code: `// Expected: denied by restrictive host access settings
result = Java.type('java.lang.System').getProperty('user.home');
`, pythonCode: '', javascriptCode: '',
  },
];
