import { useEffect, useState } from 'react';
import { Text, View, StyleSheet } from 'react-native';
import { Appfastfly } from '@appfastfly/react-native';
import type { DeepLinkEvent } from '@appfastfly/react-native';

export default function App() {
  const [deepLinkEvent, setDeepLinkEvent] = useState<DeepLinkEvent | null>(
    null
  );

  useEffect(() => {
    Appfastfly.init();

    const unsubscribe = Appfastfly.subscribe((event) => {
      setDeepLinkEvent(event);
    });

    return () => {
      unsubscribe();
    };
  }, []);

  return (
    <View style={styles.container}>
      <Text style={styles.title}>Appfastfly SDK Example</Text>
      {deepLinkEvent ? (
        <Text>Payload: {JSON.stringify(deepLinkEvent.payload, null, 2)}</Text>
      ) : (
        <Text>No deep link received yet</Text>
      )}
    </View>
  );
}

const styles = StyleSheet.create({
  container: {
    flex: 1,
    alignItems: 'center',
    justifyContent: 'center',
    padding: 20,
  },
  title: {
    fontSize: 20,
    fontWeight: 'bold',
    marginBottom: 16,
  },
});
